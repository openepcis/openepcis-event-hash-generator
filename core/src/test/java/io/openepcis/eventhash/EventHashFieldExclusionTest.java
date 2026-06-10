/*
 * Copyright 2022-2026 benelog GmbH & Co. KG
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package io.openepcis.eventhash;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the per-run field-exclusion feature ({@link EventHashGenerator#excludeFieldsInPreHash(String)}).
 * Verifies that caller-supplied fields are omitted from the pre-hash string (changing the resulting hash),
 * that the default behaviour is untouched when no extra fields are supplied, and — critically — that the
 * exclusion is per-instance: one generator's configuration must never leak into another, even under
 * concurrent use. The last guarantee is what makes the feature safe on a shared, reused instance.
 */
class EventHashFieldExclusionTest {
    private static final String JSON_FIXTURE = "2.0/EPCIS/JSON/Capture/Documents/ObjectEvent.json";
    private static final String XML_FIXTURE = "2.0/EPCIS/XML/Capture/Documents/ObjectEvent.xml";

    // A standard EPCIS field that is present in the fixtures and normally appears in the pre-hash string.
    private static final String EXCLUDED_FIELD = "bizStep";

    @Test
    void excludedFieldIsOmittedFromJsonPrehashAndChangesHash() throws IOException {
        // Baseline: the field is part of the pre-hash and therefore the hash.
        final String basePrehash = firstPrehash(JSON_FIXTURE, new EventHashGenerator());
        final String baseHash = firstHash(JSON_FIXTURE, new EventHashGenerator());
        assertTrue(basePrehash.contains(EXCLUDED_FIELD), "precondition: fixture pre-hash must contain " + EXCLUDED_FIELD);

        // Excluding it must drop it from the pre-hash and yield a different hash.
        final EventHashGenerator excluding = new EventHashGenerator();
        excluding.excludeFieldsInPreHash(EXCLUDED_FIELD);
        final String excludedPrehash = firstPrehash(JSON_FIXTURE, excluding);

        final EventHashGenerator excluding2 = new EventHashGenerator();
        excluding2.excludeFieldsInPreHash(EXCLUDED_FIELD);
        final String excludedHash = firstHash(JSON_FIXTURE, excluding2);

        assertFalse(excludedPrehash.contains(EXCLUDED_FIELD), "excluded field must not appear in the pre-hash");
        assertNotEquals(baseHash, excludedHash, "excluding a contributing field must change the hash");
    }

    @Test
    void excludedFieldIsOmittedFromXmlPrehash() throws IOException {
        final String basePrehash = firstPrehash(XML_FIXTURE, new EventHashGenerator());
        assertTrue(basePrehash.contains(EXCLUDED_FIELD), "precondition: XML fixture pre-hash must contain " + EXCLUDED_FIELD);

        final EventHashGenerator excluding = new EventHashGenerator();
        excluding.excludeFieldsInPreHash(EXCLUDED_FIELD);
        assertFalse(firstPrehash(XML_FIXTURE, excluding).contains(EXCLUDED_FIELD), "excluded field must not appear in the XML pre-hash");
    }

    @Test
    void defaultBehaviourUnchangedWhenNoExtraFieldsSupplied() throws IOException {
        final String baseline = firstHash(JSON_FIXTURE, new EventHashGenerator());

        // Blank / null / empty entries are no-ops and must not alter the hash.
        final EventHashGenerator noop = new EventHashGenerator();
        noop.excludeFieldsInPreHash("");
        noop.excludeFieldsInPreHash(null);
        noop.excludeFieldsInPreHash("   ,  ");
        assertEquals(baseline, firstHash(JSON_FIXTURE, noop), "blank exclusion input must not change the hash");
    }

    @Test
    void commaSeparatedFieldsAreTrimmedAndAllApplied() throws IOException {
        final EventHashGenerator excluding = new EventHashGenerator();
        excluding.excludeFieldsInPreHash("  bizStep , disposition ");
        final String prehash = firstPrehash(JSON_FIXTURE, excluding);
        assertFalse(prehash.contains("bizStep"), "first listed field must be excluded");
        assertFalse(prehash.contains("disposition"), "second listed field (after trimming) must be excluded");
    }

    /**
     * Per-instance isolation under concurrency: a generator configured to exclude a field, used across many
     * threads, must consistently exclude it; meanwhile a separate default generator running concurrently must
     * be completely unaffected. A regression to shared/static exclusion state would fail this.
     */
    @Test
    void exclusionIsPerInstanceUnderConcurrency() throws Throwable {
        final String defaultHash = firstHash(JSON_FIXTURE, new EventHashGenerator());

        final int threadCount = 8;
        final int iterations = 50;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            final List<Future<Throwable>> futures = new ArrayList<>();
            for (int t = 0; t < threadCount; t++) {
                final boolean excludes = t % 2 == 0; // half the threads exclude, half use defaults
                futures.add(executor.submit(() -> {
                    for (int i = 0; i < iterations; i++) {
                        final EventHashGenerator g = new EventHashGenerator();
                        if (excludes) {
                            g.excludeFieldsInPreHash(EXCLUDED_FIELD);
                            final String prehash = firstPrehash(JSON_FIXTURE, g);
                            if (prehash.contains(EXCLUDED_FIELD)) {
                                return new AssertionError("excluding instance leaked the field at iteration " + i);
                            }
                        } else {
                            final String hash = firstHash(JSON_FIXTURE, g);
                            if (!defaultHash.equals(hash)) {
                                return new AssertionError("default instance was affected by another instance's exclusion at iteration " + i);
                            }
                        }
                    }
                    return null;
                }));
            }
            for (final Future<Throwable> f : futures) {
                final Throwable err = f.get(60, TimeUnit.SECONDS);
                if (err != null) throw err;
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "executor did not shut down cleanly");
        }
    }

    // First event's pre-hash string from the given fixture. The pre-hash STRING (not a hash) is produced by the
    // multi-algorithm variant under the "prehash" key — the single-algorithm overload would hash it instead.
    private String firstPrehash(final String fixture, final EventHashGenerator generator) throws IOException {
        final String[] prehashOnly = {"prehash"};
        try (final InputStream in = open(fixture)) {
            final List<Map<String, String>> prehashes = (fixture.endsWith(".xml")
                    ? generator.fromXml(in, prehashOnly)
                    : generator.fromJson(in, prehashOnly))
                    .subscribe().asStream().toList();
            assertFalse(prehashes.isEmpty(), "fixture must produce at least one pre-hash");
            return prehashes.get(0).get("prehash");
        }
    }

    // First event's sha-256 hash from the given fixture.
    private String firstHash(final String fixture, final EventHashGenerator generator) throws IOException {
        try (final InputStream in = open(fixture)) {
            final List<String> hashes = (fixture.endsWith(".xml")
                    ? generator.fromXml(in, "sha-256")
                    : generator.fromJson(in, "sha-256"))
                    .subscribe().asStream().toList();
            assertFalse(hashes.isEmpty(), "fixture must produce at least one hash");
            return hashes.get(0);
        }
    }

    private InputStream open(final String resourcePath) {
        final InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(in, "fixture not found on classpath: " + resourcePath);
        return in;
    }
}
