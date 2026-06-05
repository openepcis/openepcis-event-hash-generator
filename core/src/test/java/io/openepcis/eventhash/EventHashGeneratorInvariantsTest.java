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

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for invariants established by the May 2026 cleanup backlog:
 * - no shared mutable state across threads
 * - caller's contextHeader Map is never mutated
 * - SAX pipeline survives a slow consumer
 * - per-thread Pattern/MessageDigest caches correct
 *
 * <p>The 34-fixture parity test validates output correctness on a single thread.
 * These tests validate the BEHAVIORAL guarantees that make concurrent + repeated
 * use safe — invisible-when-they-work, catastrophic-when-broken.
 */
class EventHashGeneratorInvariantsTest {
    // Fixture used across tests — small, deterministic, ships in openepcis-test-resources
    private static final String XML_FIXTURE = "2.0/EPCIS/XML/Capture/Documents/ObjectEvent.xml";
    private static final String JSON_FIXTURE = "2.0/EPCIS/JSON/Capture/Documents/ObjectEvent.json";

    /**
     * Concurrency: many threads hashing the same fixture must all produce the same hash.
     * Detects: singleton mutation, non-thread-safe caches, accidental shared state.
     */
    @Test
    void concurrentHashGenerationDoesNotInterfere() throws Throwable {
        // 10 threads × 100 iterations = 1000 concurrent hashes against the same fixture
        final int threadCount = 10;
        final int iterationsPerThread = 100;

        // Snapshot the expected hash on a single thread, so we have a known-good baseline
        final String expectedHash = singleThreadHash();

        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            // Each thread does N iterations independently; any divergence = test failure
            final List<Future<Throwable>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        final String actual = singleThreadHash();
                        if (!expectedHash.equals(actual)) {
                            return new AssertionError("Mismatch on iteration " + j + ": expected " + expectedHash + " got " + actual);
                        }
                    }
                    return null;
                }));
            }

            // Any non-null Future result is a concurrency failure
            for (final Future<Throwable> f : futures) {
                final Throwable err = f.get(60, TimeUnit.SECONDS);
                if (err != null) throw err;
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Executor did not shut down cleanly");
        }
    }

    /**
     * contextHeader Map must be defensively copied — caller's map is never mutated.
     * Detects: removal/regression of the `new HashMap<>(contextHeader)` boundary copy.
     */
    @Test
    void callerContextHeaderMapIsNotMutated() throws IOException {
        // Build a caller-owned map with a sentinel entry
        final Map<String, String> callerMap = new HashMap<>();
        callerMap.put("sentinel", "untouched");

        try (final InputStream json = open(JSON_FIXTURE)) {
            // Run the stream to completion — fromJson reads @context namespaces internally
            new EventHashGenerator().fromJson(json, callerMap, "sha-256").subscribe().asStream().toList();
        }

        // Caller's map must be byte-identical to what they passed in
        assertEquals(1, callerMap.size(), "EventHashGenerator must not add entries to caller's contextHeader map");
        assertEquals("untouched", callerMap.get("sentinel"), "EventHashGenerator must not modify entries in caller's contextHeader map");
    }

    /**
     * pipeline survives a slow consumer without OOM or silent drop.
     * Behavioral verification — subscribes with an artificially delayed consumer.
     * Full overflow-triggers-BackPressureFailure test needs a JVM-arg override; see test-doc below.
     */
    @Test
    void backpressureSurvivesSlowConsumer() throws Exception {
        try (final InputStream xml = open(XML_FIXTURE)) {
            // Slow consumer simulated via Mutiny's delay-by — produces backpressure demand-signals
            final List<String> results = new EventHashGenerator().fromXml(xml, "sha-256")
                    // 5ms per item — slow enough to exercise the demand path, fast enough to finish quickly
                    .onItem().call(s -> Uni.createFrom().nullItem().onItem().delayIt().by(Duration.ofMillis(5)))
                    .subscribe().asStream().toList();

            // Fixture has 2 events; the stream must complete cleanly without overflow
            assertEquals(2, results.size(), "Slow consumer must still receive all events when total < MAX_PENDING_EVENTS");
            results.forEach(hash -> assertTrue(hash.startsWith("ni:///sha-256;"), "Each event must produce a valid hash URI"));
        }
    }


    // Compute a single hash from the XML fixture on the calling thread.
    private String singleThreadHash() throws IOException {
        try (final InputStream xml = open(XML_FIXTURE)) {
            final List<Map<String, String>> hashes = new EventHashGenerator().fromXml(xml, "sha-256", "prehash").subscribe().asStream().toList();
            assertFalse(hashes.isEmpty(), "fixture must produce at least one hash");
            Map.Entry<String,String> entry = hashes.get(0).entrySet().iterator().next();
            return hashes.get(0).get(entry.getKey());
        }
    }

    // Open a fixture from openepcis-test-resources classpath.
    private InputStream open(final String resourcePath) {
        final InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(in, "fixture not found on classpath: " + resourcePath);
        return in;
    }
}
