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
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class EventHashGeneratorPublisherTest {

    private EventHashGenerator eventHashGenerator;
    private EventHashGenerator eventHashGenerator21;

    @BeforeEach
    void before() {
        eventHashGenerator = new EventHashGenerator();
        eventHashGenerator21 = new EventHashGenerator(CBVVersion.VERSION_2_1_0);
        eventHashGenerator.prehashJoin("\\n");
        eventHashGenerator21.prehashJoin("\\n");
    }

    // General test to fix bugs or necessary code modification for XML document.
    @Test
    void xmlHashGeneratorTest() {
        final InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("2.0/EPCIS/XML/Capture/Documents/ObjectEvent.xml");
        final Multi<Map<String, String>> xmlHashIds = eventHashGenerator.fromXml(xmlStream, "prehash", "sha-256");
        assertEquals(2, xmlHashIds.subscribe().asStream().toList().size());
    }

    // General test to fix bugs or necessary code modification for JSON document. Use non-default algorithm
    @Test
    void jsonHashGeneratorTest() throws IOException {
        final InputStream jsonStream = getClass().getClassLoader().getResourceAsStream("2.0/EPCIS/JSON/Capture/Documents/ObjectEvent.json");
        final Multi<Map<String, String>> jsonHashIds = eventHashGenerator.fromJson(jsonStream, "prehash", "sha3-256");
        assertEquals(2, jsonHashIds.subscribe().asStream().toList().size());
    }

    // Test to ensure invalid input data throws exception
    @Test
    void withInvalidInputData() throws IOException {
        final InputStream xmlStream = new ByteArrayInputStream("bogus-data".getBytes(StandardCharsets.UTF_8));
        final InputStream jsonStream = new ByteArrayInputStream("bogus-data".getBytes(StandardCharsets.UTF_8));

        final Stream<String> xmlHashes = eventHashGenerator.fromXml(xmlStream, "sha-512").subscribe().asStream();
        final Stream<String> jsonHashes = eventHashGenerator.fromJson(jsonStream, "sha-512").subscribe().asStream();

        assertThrows(RuntimeException.class, xmlHashes::toList);
        assertThrows(RuntimeException.class, jsonHashes::toList);
    }

    @Test
    void subMilliTimestampRoundsAndStaysInParity() throws IOException {
        // eventTime has a 4th decimal (…03.1415Z); rule 9 rounds the 3rd decimal up -> .142Z.
        final InputStream xmlStream = getClass().getResourceAsStream("/xml/SubMilliTimeStamp.xml");
        final InputStream jsonStream = getClass().getResourceAsStream("/json/SubMilliTimeStamp.json");

        final Map<String, String> xmlOut = eventHashGenerator21.fromXml(xmlStream, "prehash", "sha-256").subscribe().asStream().toList().getFirst();
        final Map<String, String> jsonOut = eventHashGenerator21.fromJson(jsonStream, "prehash", "sha-256").subscribe().asStream().toList().getFirst();

        assertTrue(xmlOut.get("prehash").contains("eventTime=2023-01-18T11:04:03.142Z"), "sub-ms should round up: " + xmlOut.get("prehash"));
        assertTrue(jsonOut.get("prehash").contains("eventTime=2023-01-18T11:04:03.142Z"), "sub-ms should round up: " + jsonOut.get("prehash"));
        assertEquals(xmlOut.get("prehash"), jsonOut.get("prehash"));
        assertEquals(xmlOut.get("sha-256"), jsonOut.get("sha-256"));
    }

    @Test
    void sensorReportGs1CurieExpandsToVocUri() throws IOException {
        final InputStream jsonStream = getClass().getResourceAsStream("/json/SensorReportGs1Curie.json");
        final Map<String, String> jsonOut = eventHashGenerator21.fromJson(jsonStream, "prehash", "sha-256").subscribe().asStream().toList().getFirst();
        // rule 15: gs1:Temperature -> https://ref.gs1.org/voc/Temperature (expanded, not stripped to bare)
        assertTrue(jsonOut.get("prehash").contains("type=https://ref.gs1.org/voc/Temperature"), "gs1: CURIE should expand: " + jsonOut.get("prehash"));
    }

    @Test
    void epcListNormalization() throws IOException {
        final InputStream jsonStream = getClass().getResourceAsStream("/json/epclist_normalisation.jsonld");
        final Map<String, String> jsonOut = eventHashGenerator.fromJson(jsonStream, "prehash", "sha-256").subscribe().asStream().toList().getFirst();

        // rule 16: epcList should be normalized to a single string with comma-separated EPCs
        assertTrue(jsonOut.get("prehash").contains("epc=https://id.gs1.org/8010/061414111111111111111-A%23%2F/8011/1234"), "CPI identifier should be normalized");
        assertTrue(jsonOut.get("prehash").contains("epc=https://id.gs1.org/01/80614141123458/21/6789%2F%26%25%22!%3F()"), "SGTIN identifier should be normalized");
    }

    @Test
    void epcListOrder() throws IOException {
        final InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("2.0/EPCIS/XML/Capture/Documents/epcisDocWithAllGS1Keys.xml");
        final InputStream jsonStream = getClass().getClassLoader().getResourceAsStream("2.0/EPCIS/JSON/Capture/Documents/epcisDocWithAllGS1Keys.json");

        final Map<String, String> xmlOut = eventHashGenerator.fromXml(xmlStream, "prehash", "sha-256").subscribe().asStream().toList().getFirst();
        final Map<String, String> jsonOut = eventHashGenerator.fromJson(jsonStream, "prehash", "sha-256").subscribe().asStream().toList().getFirst();

        assertEquals("ni:///sha-256;40744f32beff53a4bf5bb5956d465cb52de5bbcc9f131409d8efb903b04d9351?ver=CBV2.0", jsonOut.get("sha-256"));
        assertEquals(xmlOut.get("sha-256"), jsonOut.get("sha-256"));
    }

    @Test
    void defaultAndUnknownVersionsBothStampCbv2_0() throws IOException {
        // no-arg default, explicit null, and an UNREGISTERED version must all stamp ?ver=CBV2.0
        for (final EventHashGenerator g : List.of(new EventHashGenerator(), new EventHashGenerator(CBVVersion.VERSION_1_2_2))) {
            final InputStream jsonStream = getClass().getResourceAsStream("/json/DefaultVersionStamp.json");
            final Map<String, String> jsonHash = g.fromJson(jsonStream, "prehash", "sha-256").subscribe().asStream().toList().getFirst();
            final String hash = jsonHash.get("sha-256");
            assertTrue(hash.endsWith("?ver=CBV2.0"), "expected CBV2.0 suffix, got: " + hash);
        }
    }
}
