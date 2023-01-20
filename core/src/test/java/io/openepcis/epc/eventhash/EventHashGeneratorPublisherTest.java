/*
 * Copyright 2022 benelog GmbH & Co. KG
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
package io.openepcis.epc.eventhash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import io.smallrye.mutiny.Multi;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class EventHashGeneratorPublisherTest {

  @Before
  public void before() throws Exception {}

  // General test to fix bugs or necessary code modification for XML document.
  @Test
  public void xmlHashGeneratorTest() {
    final InputStream xmlStream = getClass().getResourceAsStream("/XmlEpcisEvents.xml");
    final Multi<Map<String, String>> eventHashIds =
        EventHashGenerator.fromXml(xmlStream, "prehash", "sha-256");
    eventHashIds
        .subscribe()
        .with(
            jsonHash ->
                System.out.println(jsonHash.get("sha-256") + "\n" + jsonHash.get("prehash")),
            failure -> System.out.println("XML HashId Generation Failed with " + failure),
            () -> System.out.println("Completed"));
    // assertEquals(1, eventHashIds.subscribe().asStream().toList().size());
  }

  // General test to show pre hashes for XML document.
  @Test
  public void xmlPreHashGeneratorTest() {
    final InputStream xmlStream = getClass().getResourceAsStream("/XmlEpcisEvents.xml");
    final Multi<Map<String, String>> eventHashIds =
        EventHashGenerator.fromXml(xmlStream, "prehash", "sha3-512");
    assertEquals(1, eventHashIds.subscribe().asStream().toList().size());
  }

  // General test to fix bugs or necessary code modification for JSON document.
  @Test
  public void jsonHashGeneratorTest() throws IOException {
    final InputStream jsonStream = getClass().getResourceAsStream("/JsonEpcisEvents.json");
    final List<String> eventHashIds =
        EventHashGenerator.fromJson(jsonStream, "sha3-256").subscribe().asStream().toList();
    assertEquals(1, eventHashIds.size());
  }

  // General tst to show pre hashes for JSON document.
  @Test
  public void jsonPreHashGeneratorTest() throws IOException {
    final InputStream jsonStream = getClass().getResourceAsStream("/JsonEpcisEvents.json");
    final Multi<Map<String, String>> eventHashIds =
        EventHashGenerator.fromJson(jsonStream, "prehash", "sha3-512");
    assertEquals(1, eventHashIds.subscribe().asStream().toList().size());
  }

  // Test to ensure the pre-hash string is generated correctly for simple event.
  @Test
  public void withSimpleSingleEventTest() throws IOException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.

    final InputStream xmlStream = getClass().getResourceAsStream("/SingleEvent.xml");
    final InputStream jsonStream = getClass().getResourceAsStream("/SingleEvent.json");

    final List<String> xmlHashIds =
        EventHashGenerator.fromXml(xmlStream, "sha-256").subscribe().asStream().toList();
    final List<String> jsonHashIds =
        EventHashGenerator.fromJson(jsonStream, "sha-256").subscribe().asStream().toList();

    assertEquals(xmlHashIds, jsonHashIds);
  }

  // Test to ensure the pre-hash string is generated correctly when errorDeclaration information are
  // present.
  @Test
  public void withErrorDeclarationEventTest() throws IOException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.

    final InputStream xmlStream = getClass().getResourceAsStream("/WithErrorDeclaration.xml");
    final InputStream jsonStream = getClass().getResourceAsStream("/WithErrorDeclaration.json");

    final List<String> xmlHashIds =
        EventHashGenerator.fromXml(xmlStream, "sha-256").subscribe().asStream().toList();
    final List<String> jsonHashIds =
        EventHashGenerator.fromJson(jsonStream, "sha-256").subscribe().asStream().toList();

    assertEquals(xmlHashIds, jsonHashIds);
  }

  // Test to ensure that pre-hash string is created accurately when EPCIS document contains all
  // possible fields.
  @Test
  public void withFullCombinationFieldsTest() throws IOException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.

    final InputStream xmlStream =
        getClass().getResourceAsStream("/WithFullCombinationOfFields.xml");
    final InputStream jsonStream =
        getClass().getResourceAsStream("/WithFullCombinationOfFields.json");

    final List<String> xmlHashIds =
        EventHashGenerator.fromXml(xmlStream, "sha-256").subscribe().asStream().toList();
    final List<String> jsonHashIds =
        EventHashGenerator.fromJson(jsonStream, "sha-256").subscribe().asStream().toList();

    assertEquals(xmlHashIds, jsonHashIds);
  }

  // Test to ensure that pre-hash string is created accurately when EPCIS document contains all
  // possible fields.
  @Test
  public void preFullCombinationFieldsTest() throws IOException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.

    final InputStream xmlStream =
        getClass().getResourceAsStream("/WithFullCombinationOfFields.xml");
    final InputStream jsonStream =
        getClass().getResourceAsStream("/WithFullCombinationOfFields.json");

    final Multi<Map<String, String>> xmlHashIds =
        EventHashGenerator.fromXml(xmlStream, "prehash", "sha-256");
    final Multi<Map<String, String>> jsonHashIds =
        EventHashGenerator.fromJson(jsonStream, "prehash", "sha-256");

    xmlHashIds
        .subscribe()
        .with(
            jsonHash ->
                System.out.println(jsonHash.get("sha-256") + "\n" + jsonHash.get("prehash")),
            failure -> System.out.println("XML HashId Generation Failed with " + failure),
            () -> System.out.println("Completed"));

    jsonHashIds
        .subscribe()
        .with(
            jsonHash ->
                System.out.println(jsonHash.get("sha-256") + "\n" + jsonHash.get("prehash")),
            failure -> System.out.println("XML HashId Generation Failed with " + failure),
            () -> System.out.println("Completed"));
    // assertEquals(xmlHashIds, jsonHashIds);
  }

  // Test to ensure that order of pre-hash always remains the same even when EPCIS document values
  // are jumbled up.
  @Test
  public void withJumbledOrderFieldsTest() throws IOException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.

    final InputStream xmlStream = getClass().getResourceAsStream("/withJumbledFieldsOrder.xml");
    final InputStream jsonStream = getClass().getResourceAsStream("/withJumbledFieldsOrder.json");

    final List<String> xmlHashIds =
        EventHashGenerator.fromXml(xmlStream, "sha-256").subscribe().asStream().toList();
    final List<String> jsonHashIds =
        EventHashGenerator.fromJson(jsonStream, "sha-256").subscribe().asStream().toList();

    assertEquals(xmlHashIds, jsonHashIds);
  }

  // Test to ensure order of User Extensions are always lexicographical order.
  @Test
  public void withUserExtensions() throws IOException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.

    final InputStream xmlStream = getClass().getResourceAsStream("/withUserExtensions.xml");
    final InputStream jsonStream = getClass().getResourceAsStream("/withUserExtensions.json");

    final List<String> xmlHashIds =
        EventHashGenerator.fromXml(xmlStream, "sha-256").subscribe().asStream().toList();
    final List<String> jsonHashIds =
        EventHashGenerator.fromJson(jsonStream, "sha-256").subscribe().asStream().toList();

    assertEquals(xmlHashIds, jsonHashIds);
  }

  @Test
  public void withUserExtensionsHavingEventID() throws IOException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.

    final InputStream xmlStream =
        getClass().getResourceAsStream("/withUserExtensionsHavingEventID.xml");
    final InputStream jsonStream =
        getClass().getResourceAsStream("/withUserExtensionsHavingEventID.json");

    final List<String> xmlHashIds =
        EventHashGenerator.fromXml(xmlStream, "sha-256").subscribe().asStream().toList();
    final List<String> jsonHashIds =
        EventHashGenerator.fromJson(jsonStream, "sha-256").subscribe().asStream().toList();

    assertEquals(xmlHashIds, jsonHashIds);
  }

  // Test to ensure different combination of events in single EPCIS document
  @Test
  public void withCombinationOfEvents() throws IOException {

    final InputStream xmlStream = getClass().getResourceAsStream("/EventCombination.xml");
    final InputStream jsonStream = getClass().getResourceAsStream("/EventCombination.json");

    final List<String> xmlHashIds =
        EventHashGenerator.fromXml(xmlStream, "sha-512").subscribe().asStream().toList();
    final List<String> jsonHashIds =
        EventHashGenerator.fromJson(jsonStream, "sha-512").subscribe().asStream().toList();

    assertEquals(2, xmlHashIds.size());
    assertEquals(2, jsonHashIds.size());
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
        () -> EventHashGenerator.fromXml(xmlStream, "sha-512").subscribe().asStream().toList());
    assertThrows(
        RuntimeException.class,
        () -> EventHashGenerator.fromJson(jsonStream, "sha-512").subscribe().asStream().toList());
  }

  // Test to ensure new element's exception, coordinateReferenceSystem and certificateInfo are
  // ordered
  @Test
  public void withNewElementsJsonTest() throws IOException {
    final InputStream jsonStream = getClass().getResourceAsStream("/withNewElements.json");
    final InputStream xmlStream = getClass().getResourceAsStream("/withNewElements.xml");

    EventHashGenerator.prehashJoin("\\n");

    final Multi<Map<String, String>> xmlEventHash =
        EventHashGenerator.fromXml(xmlStream, "prehash", "sha3-512");
    final String xmlHashId = xmlEventHash.subscribe().asStream().toList().get(0).get("sha3-512");

    final Multi<Map<String, String>> jsonEventHash =
        EventHashGenerator.fromJson(jsonStream, "prehash", "sha3-512");
    final String jsonHashId = jsonEventHash.subscribe().asStream().toList().get(0).get("sha3-512");

    assertEquals(xmlHashId, jsonHashId);
  }

  // Ordering of user extensions http should appear before https, JSON document
  @Test
  public void userExtensionsOrderJsonTest() throws IOException {
    final InputStream jsonStream = getClass().getResourceAsStream("/UserExtensionsOrder.json");
    EventHashGenerator.prehashJoin("\\n");
    final Multi<Map<String, String>> eventHashIds =
        EventHashGenerator.fromJson(jsonStream, "prehash", "sha3-512");
    assertEquals(1, eventHashIds.subscribe().asStream().toList().size());
  }

  @Test
  public void userExtensionsComplexOrderJsonTest() throws IOException {
    final InputStream jsonStream =
        getClass().getResourceAsStream("/UserExtensionsComplexOrder.json");
    EventHashGenerator.prehashJoin("\\n");
    final Multi<Map<String, String>> eventHashIds =
        EventHashGenerator.fromJson(jsonStream, "prehash", "sha3-512");
    assertEquals(1, eventHashIds.subscribe().asStream().toList().size());
  }

  @Test
  public void bizTransactionOrderTest() throws IOException {
    final InputStream jsonStream = getClass().getResourceAsStream("/bizTransactionOrder.json");
    EventHashGenerator.prehashJoin("\\n");
    final Multi<Map<String, String>> eventHashIds =
        EventHashGenerator.fromJson(jsonStream, "prehash", "sha3-512");
    assertEquals(1, eventHashIds.subscribe().asStream().toList().size());
  }

  @Test
  public void orderAndConversionTest() throws IOException {
    final InputStream jsonStream = getClass().getResourceAsStream("/SampleJSON.json");
    final Multi<Map<String, String>> eventHashIds =
        EventHashGenerator.fromJson(jsonStream, "prehash", "sha-256");

    EventHashGenerator.prehashJoin("\\n");
    eventHashIds
        .subscribe()
        .with(
            jsonHash ->
                System.out.println(jsonHash.get("sha-256") + "\n" + jsonHash.get("prehash")),
            failure -> System.out.println("XML HashId Generation Failed with " + failure));
    // assertEquals(1, eventHashIds.subscribe().asStream().toList().size());
  }

  @Test
  public void orderAndConversionXMLTest() throws IOException {
    final InputStream xmlStream = getClass().getResourceAsStream("/SampleFile.xml");
    final Multi<Map<String, String>> eventHashIds =
        EventHashGenerator.fromXml(xmlStream, "prehash", "sha-256");

    EventHashGenerator.prehashJoin("\\n");
    eventHashIds
        .subscribe()
        .with(
            jsonHash ->
                System.out.println(jsonHash.get("sha-256") + "\n" + jsonHash.get("prehash")),
            failure -> System.out.println("XML HashId Generation Failed with " + failure));
    // assertEquals(1, eventHashIds.subscribe().asStream().toList().size());
  }
}
