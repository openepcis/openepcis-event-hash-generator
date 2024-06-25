/*
 * Copyright 2022-2024 benelog GmbH & Co. KG
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
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class EventHashGeneratorPublisherTest {

  private EventHashGenerator eventHashGenerator;
  private EventHashGenerator eventHashGenerator2_1;

  @Before
  public void before() {
    eventHashGenerator = new EventHashGenerator();
    eventHashGenerator2_1 = new EventHashGenerator(CBVVersion.VERSION_2_1_0);
    eventHashGenerator.prehashJoin("\\n");
    eventHashGenerator2_1.prehashJoin("\\n");
  }

  // General test to fix bugs or necessary code modification for XML document.
  @Test
  public void xmlHashGeneratorTest() {

    final InputStream xmlStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/XML/Capture/Documents/ObjectEvent.xml");

    final Multi<Map<String, String>> xmlHashIds =
        eventHashGenerator.fromXml(xmlStream, "prehash", "sha-256");

    assertEquals(2, xmlHashIds.subscribe().asStream().toList().size());
  }

  // General test to show pre hashes for XML document.
  @Test
  public void xmlPreHashGeneratorTest() {
    final InputStream xmlStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/XML/Capture/Documents/ObjectEvent.xml");

    final Multi<Map<String, String>> xmlHashIds =
        eventHashGenerator.fromXml(xmlStream, "prehash", "sha3-512");

    assertEquals(2, xmlHashIds.subscribe().asStream().toList().size());
  }

  // General test to fix bugs or necessary code modification for JSON document.
  @Test
  public void jsonHashGeneratorTest() throws IOException {
    final InputStream jsonStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/JSON/Capture/Documents/ObjectEvent.json");

    final Multi<Map<String, String>> jsonHashIds =
        eventHashGenerator.fromJson(jsonStream, "prehash", "sha3-256");

    assertEquals(2, jsonHashIds.subscribe().asStream().toList().size());
  }

  // General tst to show pre hashes for JSON document.
  @Test
  public void jsonPreHashGeneratorTest() throws IOException {
    final InputStream jsonStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/JSON/Capture/Documents/ObjectEvent.json");

    final Multi<Map<String, String>> jsonHashIds =
        eventHashGenerator.fromJson(jsonStream, "prehash", "sha3-512");

    assertEquals(2, jsonHashIds.subscribe().asStream().toList().size());
  }

  // Test to ensure the pre-hash string is generated correctly for simple event.
  @Test
  public void withSimpleSingleEventTest() throws IOException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.

    final InputStream xmlStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/XML/Capture/Documents/AggregationEvent.xml");

    final InputStream jsonStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/JSON/Capture/Documents/AggregationEvent.json");

    final Multi<Map<String, String>> xmlHashIds =
        eventHashGenerator.fromXml(xmlStream, "prehash", "sha-256");
    final Multi<Map<String, String>> jsonHashIds =
        eventHashGenerator.fromJson(jsonStream, "prehash", "sha-256");

    assertEquals(
        xmlHashIds.subscribe().asStream().toList(), jsonHashIds.subscribe().asStream().toList());
  }

  // Test to ensure the pre-hash string is generated correctly when errorDeclaration information are
  // present.
  @Test
  public void withErrorDeclarationEventTest() throws IOException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.
    final InputStream xmlStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/XML/Capture/Documents/ObjectEvent_with_error_declaration.xml");

    final InputStream jsonStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/JSON/Capture/Documents/ObjectEvent_with_error_declaration.json");

    final Multi<Map<String, String>> xmlHashIds =
        eventHashGenerator.fromXml(xmlStream, "prehash", "sha-256");
    final Multi<Map<String, String>> jsonHashIds =
        eventHashGenerator.fromJson(jsonStream, "prehash", "sha-256");

    assertEquals(
        xmlHashIds.subscribe().asStream().toList(), jsonHashIds.subscribe().asStream().toList());
  }

  // Test to ensure that pre-hash string is created accurately when EPCIS document contains all
  // possible fields.
  @Test
  public void withFullCombinationFieldsTest() throws IOException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.

    final InputStream xmlStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/XML/Capture/Documents/AggregationEvent_all_possible_fields.xml");
    final InputStream jsonStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/JSON/Capture/Documents/AggregationEvent_all_possible_fields.json");

    final List<String> xmlHashIds =
        eventHashGenerator.fromXml(xmlStream, "sha-256").subscribe().asStream().toList();
    final List<String> jsonHashIds =
        eventHashGenerator.fromJson(jsonStream, "sha-256").subscribe().asStream().toList();

    assertEquals(xmlHashIds, jsonHashIds);
  }

  // Test to ensure that order of pre-hash always remains the same even when EPCIS document values
  // are jumbled up.
  @Test
  public void withJumbledOrderFieldsTest() throws IOException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.
    // src/main/resources/2.0/EPCIS/XML/Capture/Documents/JumbledFieldsOrder.xml

    final InputStream xmlStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/XML/Capture/Documents/JumbledFieldsOrder.xml");
    final InputStream jsonStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/JSON/Capture/Documents/JumbledFieldsOrder.json");

    final List<String> xmlHashIds =
        eventHashGenerator.fromXml(xmlStream, "sha-256").subscribe().asStream().toList();
    final List<String> jsonHashIds =
        eventHashGenerator.fromJson(jsonStream, "sha-256").subscribe().asStream().toList();

    assertEquals(xmlHashIds, jsonHashIds);
  }

  // Test to ensure order of User Extensions are always lexicographical order.
  @Test
  public void withUserExtensions() throws IOException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.

    final InputStream xmlStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/XML/Capture/Documents/AggregationEvent_with_userExtensions.xml");
    final InputStream jsonStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/JSON/Capture/Documents/AggregationEvent_with_userExtensions.json");

    final List<String> xmlHashIds =
        eventHashGenerator.fromXml(xmlStream, "sha-256").subscribe().asStream().toList();
    final List<String> jsonHashIds =
        eventHashGenerator.fromJson(jsonStream, "sha-256").subscribe().asStream().toList();

    assertEquals(xmlHashIds, jsonHashIds);
  }

  @Test
  public void withUserExtensionsHavingEventID() throws IOException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.
    final InputStream xmlStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/XML/Capture/Documents/TransformationEvent_with_userExtensions.xml");

    final InputStream jsonStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/JSON/Capture/Documents/TransformationEvent_with_userExtensions.json");

    // getClass().getResourceAsStream("/withUserExtensionsHavingEventID.json");

    final List<String> xmlHashIds =
        eventHashGenerator.fromXml(xmlStream, "sha-256").subscribe().asStream().toList();
    final List<String> jsonHashIds =
        eventHashGenerator.fromJson(jsonStream, "sha-256").subscribe().asStream().toList();

    assertEquals(xmlHashIds, jsonHashIds);
  }

  // Test to ensure different combination of events in single EPCIS document
  @Test
  public void withCombinationOfEvents() throws IOException {
    final InputStream xmlStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/XML/Capture/Documents/Combination_of_different_event.xml");
    final InputStream jsonStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/JSON/Capture/Documents/Combination_of_different_event.json");

    final List<String> xmlHashIds =
        eventHashGenerator.fromXml(xmlStream, "sha-512").subscribe().asStream().toList();
    final List<String> jsonHashIds =
        eventHashGenerator.fromJson(jsonStream, "sha-512").subscribe().asStream().toList();

    assertEquals(xmlHashIds, jsonHashIds);
  }

  // Test to ensure invalid input data throws exception
  @Test
  public void withInvalidInputData() {
    final InputStream xmlStream =
        new ByteArrayInputStream("bogus-data".getBytes(StandardCharsets.UTF_8));
    final InputStream jsonStream =
        new ByteArrayInputStream("bogus-data".getBytes(StandardCharsets.UTF_8));

    assertThrows(
        RuntimeException.class,
        () -> eventHashGenerator.fromXml(xmlStream, "sha-512").subscribe().asStream().toList());
    assertThrows(
        RuntimeException.class,
        () -> eventHashGenerator.fromJson(jsonStream, "sha-512").subscribe().asStream().toList());
  }

  // Test to ensure new element's exception, coordinateReferenceSystem and certificateInfo are
  // ordered
  @Test
  public void withNewElementsJsonTest() throws IOException {

    final InputStream jsonStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/JSON/Capture/Documents/ObjectEvent_with_sensorData.json");
    final InputStream xmlStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/XML/Capture/Documents/ObjectEvent_with_sensorData.xml");

    final Multi<Map<String, String>> xmlEventHash =
        eventHashGenerator.fromXml(xmlStream, "prehash", "sha3-512");
    final Multi<Map<String, String>> jsonEventHash =
        eventHashGenerator.fromJson(jsonStream, "prehash", "sha3-512");

    assertEquals(
        xmlEventHash.subscribe().asStream().toList(),
        jsonEventHash.subscribe().asStream().toList());
  }

  // Ordering of user extensions http should appear before https, JSON document
  @Test
  public void userExtensionsOrderJsonTest() throws IOException {
    // Check the order for user extensions in XML and JSON is same

    final InputStream xmlStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/XML/Capture/Documents/ObjectEvent_with_userExtensions.xml");
    final InputStream jsonStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/JSON/Capture/Documents/ObjectEvent_with_userExtensions.json");

    final Multi<Map<String, String>> xmlEventHash =
        eventHashGenerator.fromXml(xmlStream, "prehash", "sha3-512");
    final Multi<Map<String, String>> jsonEventHash =
        eventHashGenerator.fromJson(jsonStream, "prehash", "sha3-512");

    assertEquals(
        xmlEventHash.subscribe().asStream().toList(),
        jsonEventHash.subscribe().asStream().toList());
  }

  @Test
  public void bizTransactionOrderTest() throws IOException {

    final InputStream xmlStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/XML/Capture/Documents/TransactionEvent_with_userExtensions.xml");
    final InputStream jsonStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/JSON/Capture/Documents/TransactionEvent_with_userExtensions.json");

    final Multi<Map<String, String>> xmlEventHash =
        eventHashGenerator.fromXml(xmlStream, "prehash", "sha3-512");
    final Multi<Map<String, String>> jsonEventHash =
        eventHashGenerator.fromJson(jsonStream, "prehash", "sha3-512");

    assertEquals(
        xmlEventHash.subscribe().asStream().toList(),
        jsonEventHash.subscribe().asStream().toList());
  }

  @Test
  public void fullCombinationOrderAggregationEventTest() throws IOException {

    final InputStream xmlStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/XML/Capture/Documents/AggregationEvent_all_possible_fields.xml");
    final InputStream jsonStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/JSON/Capture/Documents/AggregationEvent_all_possible_fields.json");

    final Multi<Map<String, String>> xmlEventHash =
        eventHashGenerator.fromXml(xmlStream, "prehash", "sha-256");
    final Multi<Map<String, String>> jsonEventHash =
        eventHashGenerator.fromJson(jsonStream, "prehash", "sha-256");

    assertEquals(
        xmlEventHash.subscribe().asStream().toList(),
        jsonEventHash.subscribe().asStream().toList());
  }

  @Test
  public void fullCombinationOrderTransactionEventTest() throws IOException {

    final InputStream xmlStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/XML/Capture/Documents/TransactionEvent_all_possible_fields.xml");
    final InputStream jsonStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/JSON/Capture/Documents/TransactionEvent_all_possible_fields.json");

    final Multi<Map<String, String>> xmlEventHash =
        eventHashGenerator.fromXml(xmlStream, "prehash", "sha-256");
    final Multi<Map<String, String>> jsonEventHash =
        eventHashGenerator.fromJson(jsonStream, "prehash", "sha-256");

    assertEquals(
        xmlEventHash.subscribe().asStream().toList(),
        jsonEventHash.subscribe().asStream().toList());
  }

  @Test
  public void curieStringTest() throws IOException {

    final InputStream xmlStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/XML/Capture/Documents/CurieString_document.xml");
    final InputStream jsonStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/JSON/Capture/Documents/CurieString_document.json");

    final Multi<Map<String, String>> xmlEventHash =
        eventHashGenerator.fromXml(xmlStream, "prehash", "sha-256");
    final Multi<Map<String, String>> jsonEventHash =
        eventHashGenerator.fromJson(jsonStream, "prehash", "sha-256");

    assertEquals(
        xmlEventHash.subscribe().asStream().toList(),
        jsonEventHash.subscribe().asStream().toList());
  }

  @Test
  public void sensorDataTest() throws IOException {

    final InputStream xmlStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/XML/Capture/Documents/SensorData_with_combined_events.xml");
    final InputStream jsonStream =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/JSON/Capture/Documents/SensorData_with_combined_events.json");

    final Multi<Map<String, String>> xmlEventHash =
        eventHashGenerator.fromXml(xmlStream, "prehash", "sha-256");
    final Multi<Map<String, String>> jsonEventHash =
        eventHashGenerator.fromJson(jsonStream, "prehash", "sha-256");

    assertEquals(
        xmlEventHash.subscribe().asStream().toList(),
        jsonEventHash.subscribe().asStream().toList());
  }

  // Compare EPCIS Document with EPCIS Query Document
  @Test
  public void combinationJSONQueryDocumentTest() throws IOException {
    final InputStream epcisDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/JSON/Capture/Documents/Combination_of_different_event.json");
    final InputStream epcisQueryDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/JSON/Query/Combination_of_different_event.json");

    final Multi<Map<String, String>> documentEventHash =
        eventHashGenerator.fromJson(epcisDocument, "prehash", "sha-256");
    final Multi<Map<String, String>> queryEventHash =
        eventHashGenerator.fromJson(epcisQueryDocument, "prehash", "sha-256");

    assertEquals(
        documentEventHash.subscribe().asStream().toList(),
        queryEventHash.subscribe().asStream().toList());
    // documentEventHash.subscribe().with(dHash -> System.out.println(dHash.get("sha-256") + "\n" +
    // dHash.get("prehash") + "\n\n"), failure -> System.out.println("Document HashId Generation
    // Failed with " + failure));
  }

  @Test
  public void curieJSONQueryDocumentTest() throws IOException {
    final InputStream epcisDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/JSON/Capture/Documents/CurieString_document.json");
    final InputStream epcisQueryDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/JSON/Query/CurieString_document.json");

    final Multi<Map<String, String>> documentEventHash =
        eventHashGenerator.fromJson(epcisDocument, "prehash", "sha-256");
    final Multi<Map<String, String>> queryEventHash =
        eventHashGenerator.fromJson(epcisQueryDocument, "prehash", "sha-256");

    assertEquals(
        documentEventHash.subscribe().asStream().toList(),
        queryEventHash.subscribe().asStream().toList());
  }

  @Test
  public void jumbledJSONOrderQueryDocumentTest() throws IOException {
    final InputStream epcisDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/JSON/Capture/Documents/JumbledFieldsOrder.json");
    final InputStream epcisQueryDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/JSON/Query/JumbledFieldsOrder.json");

    final Multi<Map<String, String>> documentEventHash =
        eventHashGenerator.fromJson(epcisDocument, "prehash", "sha-256");
    final Multi<Map<String, String>> queryEventHash =
        eventHashGenerator.fromJson(epcisQueryDocument, "prehash", "sha-256");

    assertEquals(
        documentEventHash.subscribe().asStream().toList(),
        queryEventHash.subscribe().asStream().toList());
  }

  @Test
  public void sensorDataJSONnQueryDocumentTest() throws IOException {
    final InputStream epcisDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/JSON/Capture/Documents/SensorData_with_combined_events.json");
    final InputStream epcisQueryDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/JSON/Query/SensorData_with_combined_events.json");

    final Multi<Map<String, String>> documentEventHash =
        eventHashGenerator.fromJson(epcisDocument, "prehash", "sha-256");
    final Multi<Map<String, String>> queryEventHash =
        eventHashGenerator.fromJson(epcisQueryDocument, "prehash", "sha-256");

    assertEquals(
        documentEventHash.subscribe().asStream().toList(),
        queryEventHash.subscribe().asStream().toList());
  }

  @Test
  public void errorDeclarationJSONQueryDocumentTest() throws IOException {
    final InputStream epcisDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/JSON/Capture/Documents/TransformationEvent_with_error_declaration.json");
    final InputStream epcisQueryDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/JSON/Query/TransformationEvent_with_error_declaration.json");

    final Multi<Map<String, String>> documentEventHash =
        eventHashGenerator.fromJson(epcisDocument, "prehash", "sha-256");
    final Multi<Map<String, String>> queryEventHash =
        eventHashGenerator.fromJson(epcisQueryDocument, "prehash", "sha-256");

    assertEquals(
        documentEventHash.subscribe().asStream().toList(),
        queryEventHash.subscribe().asStream().toList());
  }

  @Test
  public void combinationXMLQueryDocumentTest() {
    final InputStream epcisDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/XML/Capture/Documents/Combination_of_different_event.xml");
    final InputStream epcisQueryDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/XML/Query/Combination_of_different_event.xml");

    final Multi<Map<String, String>> documentEventHash =
        eventHashGenerator.fromXml(epcisDocument, "prehash", "sha-256");
    final Multi<Map<String, String>> queryEventHash =
        eventHashGenerator.fromXml(epcisQueryDocument, "prehash", "sha-256");

    assertEquals(
        documentEventHash.subscribe().asStream().toList(),
        queryEventHash.subscribe().asStream().toList());
  }

  @Test
  public void curieXMLQueryDocumentTest() {
    final InputStream epcisDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/XML/Capture/Documents/CurieString_document.xml");
    final InputStream epcisQueryDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/XML/Query/CurieString_document.xml");

    final Multi<Map<String, String>> documentEventHash =
        eventHashGenerator.fromXml(epcisDocument, "prehash", "sha-256");
    final Multi<Map<String, String>> queryEventHash =
        eventHashGenerator.fromXml(epcisQueryDocument, "prehash", "sha-256");

    assertEquals(
        documentEventHash.subscribe().asStream().toList(),
        queryEventHash.subscribe().asStream().toList());
  }

  @Test
  public void jumbledXMLOrderQueryDocumentTest() {
    final InputStream epcisDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/XML/Capture/Documents/JumbledFieldsOrder.xml");
    final InputStream epcisQueryDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/XML/Query/JumbledFieldsOrder.xml");

    final Multi<Map<String, String>> documentEventHash =
        eventHashGenerator.fromXml(epcisDocument, "prehash", "sha-256");
    final Multi<Map<String, String>> queryEventHash =
        eventHashGenerator.fromXml(epcisQueryDocument, "prehash", "sha-256");

    assertEquals(
        documentEventHash.subscribe().asStream().toList(),
        queryEventHash.subscribe().asStream().toList());
  }

  @Test
  public void sensorDataXMLQueryDocumentTest() {
    final InputStream epcisDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream(
                "2.0/EPCIS/XML/Capture/Documents/SensorData_with_combined_events.xml");
    final InputStream epcisQueryDocument =
        getClass()
            .getClassLoader()
            .getResourceAsStream("2.0/EPCIS/XML/Query/SensorData_with_combined_events.xml");

    final Multi<Map<String, String>> documentEventHash =
        eventHashGenerator.fromXml(epcisDocument, "prehash", "sha-256");
    final Multi<Map<String, String>> queryEventHash =
        eventHashGenerator.fromXml(epcisQueryDocument, "prehash", "sha-256");

    assertEquals(
        documentEventHash.subscribe().asStream().toList(),
        queryEventHash.subscribe().asStream().toList());
  }

  @Test
  public void errorDeclarationXMLQueryDocumentTest() throws IOException {
    final InputStream xmlDocument =
            getClass()
                    .getClassLoader()
                    .getResourceAsStream(
                            "2.0/EPCIS/XML/Capture/Documents/TransformationEvent_all_possible_fields.xml");
    final InputStream jsonDocument =
            getClass()
                    .getClassLoader()
                    .getResourceAsStream(
                            "2.0/EPCIS/JSON/Capture/Documents/TransformationEvent_all_possible_fields.json");


    final Multi<Map<String, String>> documentEventHash =
        eventHashGenerator.fromXml(xmlDocument, "prehash", "sha-256");
    final Multi<Map<String, String>> queryEventHash =
        eventHashGenerator.fromJson(jsonDocument, "prehash", "sha-256");

    assertEquals(
        documentEventHash.subscribe().asStream().toList(),
        queryEventHash.subscribe().asStream().toList());
  }

  @Test
  public void xmlVsJsonCaptureDocument() throws IOException {
    final InputStream xmlDocument =
            getClass()
                    .getClassLoader()
                    .getResourceAsStream(
                            "2.0/EPCIS/XML/Capture/Documents/TransformationEvent_with_userExtensions.xml");
    final InputStream jsonDocument =
            getClass()
                    .getClassLoader()
                    .getResourceAsStream(
                            "2.0/EPCIS/JSON/Capture/Documents/TransformationEvent_with_userExtensions.json");

    final Multi<Map<String, String>> documentEventHash =
            eventHashGenerator.fromXml(xmlDocument, "prehash", "sha-256");
    final Multi<Map<String, String>> queryEventHash =
            eventHashGenerator.fromJson(jsonDocument, "prehash", "sha-256");

    assertEquals(
            documentEventHash.subscribe().asStream().toList(),
            queryEventHash.subscribe().asStream().toList());
  }

  @Test
  public void objectEventAllFieldsTest() throws IOException {
    final InputStream xmlDocument =
            getClass()
                    .getClassLoader()
                    .getResourceAsStream(
                            "2.0/EPCIS/XML/Capture/Documents/ObjectEvent_all_possible_fields.xml");
    final InputStream jsonDocument =
            getClass()
                    .getClassLoader()
                    .getResourceAsStream(
                            "2.0/EPCIS/JSON/Capture/Documents/ObjectEvent_all_possible_fields.json");

    final Multi<Map<String, String>> xmlEventHash = eventHashGenerator.fromXml(xmlDocument, "prehash", "sha-256");
    final Multi<Map<String, String>> jsonEventHash = eventHashGenerator.fromJson(jsonDocument, "prehash", "sha-256");

    assertEquals(xmlEventHash.subscribe().asStream().toList(), jsonEventHash.subscribe().asStream().toList());
  }

  @Test
  public void transformationEventAllFieldsTest() throws IOException {
    final InputStream xmlDocument =
            getClass()
                    .getClassLoader()
                    .getResourceAsStream(
                            "2.0/EPCIS/XML/Capture/Documents/TransformationEvent_all_possible_fields.xml");
    final InputStream jsonDocument =
            getClass()
                    .getClassLoader()
                    .getResourceAsStream(
                            "2.0/EPCIS/JSON/Capture/Documents/TransformationEvent_all_possible_fields.json");

    final Multi<Map<String, String>> xmlEventHash = eventHashGenerator.fromXml(xmlDocument, "prehash", "sha-256");
    final Multi<Map<String, String>> jsonEventHash = eventHashGenerator.fromJson(jsonDocument, "prehash", "sha-256");

    assertEquals(xmlEventHash.subscribe().asStream().toList(), jsonEventHash.subscribe().asStream().toList());
  }

  @Test
  public void transformationEventAllFieldsTestVersion2_1() throws IOException {
    final InputStream xmlDocument = getClass().getClassLoader().getResourceAsStream("2.0/EPCIS/XML/Capture/Documents/TransformationEvent_all_possible_fields.xml");
    final InputStream jsonDocument = getClass().getClassLoader().getResourceAsStream("2.0/EPCIS/JSON/Capture/Documents/TransformationEvent_all_possible_fields.json");

    final Multi<Map<String, String>> xmlEventHash = eventHashGenerator2_1.fromXml(xmlDocument, "prehash", "sha-256");
    final Multi<Map<String, String>> jsonEventHash = eventHashGenerator2_1.fromJson(jsonDocument, "prehash", "sha-256");

    assertEquals(xmlEventHash.subscribe().asStream().toList(), jsonEventHash.subscribe().asStream().toList());
  }

  @Test
  public void xmlVsJsonCaptureDocumentVersion2_1() throws IOException {
    final InputStream xmlDocument = getClass().getClassLoader().getResourceAsStream("2.0/EPCIS/XML/Capture/Documents/TransformationEvent_with_userExtensions.xml");
    final InputStream jsonDocument = getClass().getClassLoader().getResourceAsStream("2.0/EPCIS/JSON/Capture/Documents/TransformationEvent_with_userExtensions.json");

    final Multi<Map<String, String>> documentEventHash = eventHashGenerator2_1.fromXml(xmlDocument, "prehash", "sha-256");
    final Multi<Map<String, String>> queryEventHash = eventHashGenerator2_1.fromJson(jsonDocument, "prehash", "sha-256");

    assertEquals(documentEventHash.subscribe().asStream().toList(), queryEventHash.subscribe().asStream().toList());
  }
}