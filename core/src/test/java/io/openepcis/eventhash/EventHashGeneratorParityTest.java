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

import io.openepcis.constants.CBVVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The same EPCIS event in XML and in JSON/JSON-LD must produce the same hash.
 * Runs the test for various JSON/XML documents in openepcis-test-resources
 */
class EventHashGeneratorParityTest {
    private static final String RESOURCE_INDEX = "openepcis-test-resources.list";

    // Path for EPCIS Capture documents parity test
    private static final String JSON_DIR = "2.0/EPCIS/JSON/Capture/Documents/";
    private static final String XML_DIR = "2.0/EPCIS/XML/Capture/Documents/";

    // Path for EPCIS Query documents party test
    private static final String JSON_QUERY_DIR = "2.0/EPCIS/JSON/Query/";
    private static final String XML_QUERY_DIR = "2.0/EPCIS/XML/Query/";

    private static final String PREHASH = "prehash";
    private static final String ALGORITHM = "sha-256";

    // Paid the base name available for XML and JSON with each CBV Version
    static Stream<Arguments> capturePairs() throws IOException {
        final List<String> index = readResourceIndex();
        final Set<String> xmlNames = baseNames(index, XML_DIR, ".xml");
        final Set<String> pairs = new TreeSet<>(baseNames(index, JSON_DIR, ".json"));
        pairs.retainAll(xmlNames);

        assertFalse(pairs.isEmpty(), "No test resources found in the classpath: " + RESOURCE_INDEX);

        return pairs.stream()
                .flatMap(base -> Stream.of(CBVVersion.VERSION_2_0_0, CBVVersion.VERSION_2_1_0).map(version -> Arguments.of(base, version)));
    }

    @ParameterizedTest(name = "[CBV {1}] {0}")
    @MethodSource("capturePairs")
    void xmlAndJsonHashesMatch(final String baseName, final CBVVersion version) throws IOException {
        final List<Map<String, String>> xml = hashEvents(XML_DIR + baseName + ".xml", true, version);
        final List<Map<String, String>> json = hashEvents(JSON_DIR + baseName + ".json", false, version);
        final String document = baseName + " [CBV " + version.getVersion() + "]";

        assertFalse(xml.isEmpty(), document + ": the XML document produced no events");
        assertFalse(json.isEmpty(), document + ": the JSON document produced no events");
        assertEquals(xml.size(), json.size(), document + ": event count differs between XML and JSON");

        assertAll(IntStream.range(0, xml.size()).mapToObj(i -> () -> {
            final Map<String, String> x = xml.get(i);
            final Map<String, String> j = json.get(i);
            final String event = document + ", event " + (i + 1) + " of " + xml.size();

            assertEquals(x.get(ALGORITHM), j.get(ALGORITHM),
                    () -> event + ": hash differs"
                            + "\n---------- XML pre-hash ----------\n" + x.get(PREHASH)
                            + "\n---------- JSON pre-hash ---------\n" + j.get(PREHASH)
                            + "\n----------------------------------");

            assertTrue(j.get(ALGORITHM).endsWith(expectedSuffix(version)),
                    () -> event + ": expected the hash to end with " + expectedSuffix(version) + " but was " + j.get(ALGORITHM));
        }));
    }

    // Every document published both as a capture document and as a query document, in the same format
    static Stream<Arguments> captureAndQueryPairs() throws IOException {
        final List<String> index = readResourceIndex();

        return Stream.of(
                        // label, capture folder, query folder, extension
                        new String[]{"XML", XML_DIR, XML_QUERY_DIR, ".xml"},
                        new String[]{"JSON", JSON_DIR, JSON_QUERY_DIR, ".json"})
                .flatMap(format -> {
                    // names present as a capture document...
                    final Set<String> shared = new TreeSet<>(baseNames(index, format[1], format[3]));
                    // ...kept only if the same name is also a query document
                    shared.retainAll(baseNames(index, format[2], format[3]));
                    return shared.stream()
                            .flatMap(base -> Stream.of(CBVVersion.VERSION_2_0_0, CBVVersion.VERSION_2_1_0)
                                    .map(version -> Arguments.of(format[0], base, version)));
                });
    }


    @ParameterizedTest(name = "[{0}, CBV {2}] {1}")
    @MethodSource("captureAndQueryPairs")
    void captureAndQueryDocumentHashesMatch(final String format, final String baseName, final CBVVersion version) throws IOException {
        final boolean xml = "XML".equals(format);                       // one flag drives path and parser choice
        final String extension = xml ? ".xml" : ".json";
        final String captureDir = xml ? XML_DIR : JSON_DIR;
        final String queryDir = xml ? XML_QUERY_DIR : JSON_QUERY_DIR;
        final String document = format + " " + baseName + " [CBV " + version.getVersion() + "]";

        final List<Map<String, String>> capture = hashEvents(captureDir + baseName + extension, xml, version);
        final List<Map<String, String>> query = hashEvents(queryDir + baseName + extension, xml, version);

        assertFalse(capture.isEmpty(), document + ": the capture document produced no events");
        assertFalse(query.isEmpty(), document + ": the query document produced no events");
        assertEquals(capture.size(), query.size(), document + ": event count differs between the capture and query document");

        assertAll(IntStream.range(0, capture.size()).mapToObj(i -> () -> {
            final Map<String, String> c = capture.get(i);
            final Map<String, String> q = query.get(i);
            final String event = document + ", event " + (i + 1) + " of " + capture.size();

            assertEquals(c.get(ALGORITHM), q.get(ALGORITHM),
                    () -> event + ": hash differs"                      // lazy message: built only on failure
                            + "\n-------- capture pre-hash --------\n" + c.get(PREHASH)
                            + "\n-------- query pre-hash ----------\n" + q.get(PREHASH)
                            + "\n----------------------------------");
        }));
    }

    // Keeps JSON-only documents visible instead of silently untested.
    @Test
    void reportJsonCaptureDocumentsWithoutXmlCounterpart() throws IOException {
        final List<String> index = readResourceIndex();
        final Set<String> jsonOnly = new TreeSet<>(baseNames(index, JSON_DIR, ".json"));
        jsonOnly.removeAll(baseNames(index, XML_DIR, ".xml"));
        System.out.println("JSON capture documents without an XML counterpart: " + jsonOnly);
        assertNotNull(jsonOnly);
    }

    private static String expectedSuffix(final CBVVersion version) {
        return CBVVersion.VERSION_2_1_0.equals(version) ? "?ver=CBV2.1" : "?ver=CBV2.0";
    }

    private static List<Map<String, String>> hashEvents(final String path, final boolean xml, final CBVVersion version) throws IOException {
        final EventHashGenerator generator = new EventHashGenerator(version);
        generator.prehashJoin("\\n");

        try (InputStream in = EventHashGeneratorParityTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "document missing from the classpath: " + path);
            return (xml ? generator.fromXml(in, PREHASH, ALGORITHM) : generator.fromJson(in, PREHASH, ALGORITHM)).subscribe().asStream().toList();
        }
    }

    private static List<String> readResourceIndex() throws IOException {
        try (InputStream in = EventHashGeneratorParityTest.class.getClassLoader().getResourceAsStream(RESOURCE_INDEX)) {
            assertNotNull(in, "resource index missing from the classpath: " + RESOURCE_INDEX);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
        }
    }

    private static Set<String> baseNames(final List<String> index, final String directory, final String extension) {
        final Set<String> names = new HashSet<>();

        for (final String line : index) {
            final String path = line.startsWith("/") ? line.substring(1) : line;

            if (path.startsWith(directory) && path.endsWith(extension)) {
                final String name = path.substring(directory.length(), path.length() - extension.length());

                // Direct children only: a Query folder also holds Documents/ and Queries/ subfolders.
                if (!name.contains("/")) {
                    names.add(name);
                }
            }
        }
        return names;
    }
}
