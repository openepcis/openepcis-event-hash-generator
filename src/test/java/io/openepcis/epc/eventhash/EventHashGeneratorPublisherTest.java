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
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class EventHashGeneratorPublisherTest {

  private static final String PYTHON_TOOL_API = "https://event-hash-generator.openepcis.io/hash";
  private static final HttpRequest.Builder XML_REQUEST_BUILDER =
      HttpRequest.newBuilder(URI.create(PYTHON_TOOL_API)).header("content-type", "application/xml");
  private static final HttpRequest.Builder JSON_REQUEST_BUILDER =
      HttpRequest.newBuilder(URI.create(PYTHON_TOOL_API))
          .header("content-type", "application/json");

  // General test to fix bugs or necessary code modification for XML document.
  @Test
  public void xmlHashGeneratorTest() {
    final InputStream xmlStream = getClass().getResourceAsStream("/XmlEpcisEvents.xml");
    final List<String> eventHashIds =
        EventHashGenerator.fromXml(xmlStream, "sha-256").subscribe().asStream().toList();
    assertEquals(1, eventHashIds.size());
    System.out.println("\nXML document Generated Event Hash Ids : \n" + eventHashIds);
  }

  // General test to show pre hashes for XML document.
  @Test
  public void xmlPreHashGeneratorTest() {
    final InputStream xmlStream = getClass().getResourceAsStream("/XmlEpcisEvents.xml");
    final Multi<Map<String, String>> eventHashIds =
        EventHashGenerator.fromXml(xmlStream, new String[] {"prehash", "sha3-512"});
    assertEquals(1, eventHashIds.subscribe().asStream().toList().size());
    /*
        eventHashIds.subscribe().with(
        xmlHash -> System.out.println(xmlHash),
        failure -> System.out.println("JSON HashId Generation Failed with " + failure));
    */
  }

  // General test to fix bugs or necessary code modification for JSON document.
  @Test
  public void jsonHashGeneratorTest() throws IOException {
    final InputStream jsonStream = getClass().getResourceAsStream("/JsonEpcisEvents.json");
    final List<String> eventHashIds =
        EventHashGenerator.fromJson(jsonStream, "sha3-256").subscribe().asStream().toList();
    assertEquals(1, eventHashIds.size());
    System.out.println("\nJSON/JSON-LD document Generated Event Hash Ids : \n" + eventHashIds);
  }

  // General tst to show pre hashes for JSON document.
  @Test
  public void jsonPreHashGeneratorTest() throws IOException {
    final InputStream jsonStream = getClass().getResourceAsStream("/JsonEpcisEvents.json");
    // EventHashGenerator.prehashJoin("\\n");
    final Multi<Map<String, String>> eventHashIds =
        EventHashGenerator.fromJson(jsonStream, new String[] {"prehash", "sha3-512"});
    assertEquals(1, eventHashIds.subscribe().asStream().toList().size());

    /*eventHashIds
    .subscribe()
    .with(
        jsonHash -> System.out.println(jsonHash),
        failure -> System.out.println("JSON HashId Generation Failed with " + failure));*/
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

    // Check if the response from Python Event Hash Generator is same as Java tool
    // final HttpRequest xmlRequest =
    // XML_REQUEST_BUILDER.POST(HttpRequest.BodyPublishers.ofString(new
    // String(getClass().getResourceAsStream("/SingleEvent.xml").readAllBytes(),
    // StandardCharsets.UTF_8))).build();
    // assertEquals(HttpClient.newHttpClient().send(xmlRequest,
    // HttpResponse.BodyHandlers.ofString()).body(), xmlHashIds.get(0));

    // Check if the response from Python Event Hash Generator is same as Java tool
    // final HttpRequest jsonRequest =
    // JSON_REQUEST_BUILDER.POST(HttpRequest.BodyPublishers.ofString(new
    // String(getClass().getResourceAsStream("/SingleEvent.json").readAllBytes(),
    // StandardCharsets.UTF_8))).build();
    // assertEquals(HttpClient.newHttpClient().send(jsonRequest,
    // HttpResponse.BodyHandlers.ofString()).body(), jsonHashIds.get(0));
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

    // Check if the response from Python Event Hash Generator is same as Java tool
    // final HttpRequest xmlRequest =
    // XML_REQUEST_BUILDER.POST(HttpRequest.BodyPublishers.ofString(new
    // String(getClass().getResourceAsStream("/WithErrorDeclaration.xml").readAllBytes(),
    // StandardCharsets.UTF_8))).build();
    // assertEquals(HttpClient.newHttpClient().send(xmlRequest,
    // HttpResponse.BodyHandlers.ofString()).body(), xmlHashIds.get(0));

    // Check if the response from Python Event Hash Generator is same as Java tool
    // final HttpRequest jsonRequest =
    // JSON_REQUEST_BUILDER.POST(HttpRequest.BodyPublishers.ofString(new
    // String(getClass().getResourceAsStream("/WithErrorDeclaration.json").readAllBytes(),
    // StandardCharsets.UTF_8))).build();
    // assertEquals(HttpClient.newHttpClient().send(jsonRequest,
    // HttpResponse.BodyHandlers.ofString()).body(), jsonHashIds.get(0));
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

    // Check if the response from Python Event Hash Generator is same as Java tool
    // final HttpRequest xmlRequest =
    // XML_REQUEST_BUILDER.POST(HttpRequest.BodyPublishers.ofString(new
    // String(getClass().getResourceAsStream("/WithFullCombinationOfFields.xml").readAllBytes(),
    // StandardCharsets.UTF_8))).build();
    // assertEquals(HttpClient.newHttpClient().send(xmlRequest,
    // HttpResponse.BodyHandlers.ofString()).body(), xmlHashIds.get(0));

    // Check if the response from Python Event Hash Generator is same as Java tool
    // final HttpRequest jsonRequest =
    // JSON_REQUEST_BUILDER.POST(HttpRequest.BodyPublishers.ofString(new
    // String(getClass().getResourceAsStream("/WithFullCombinationOfFields.json").readAllBytes(),
    // StandardCharsets.UTF_8))).build();
    // assertEquals(HttpClient.newHttpClient().send(jsonRequest,
    // HttpResponse.BodyHandlers.ofString()).body(), jsonHashIds.get(0));
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

    final List<String> xmlHashIds =
        EventHashGenerator.fromXml(xmlStream, "prehash").subscribe().asStream().toList();
    final List<String> jsonHashIds =
        EventHashGenerator.fromJson(jsonStream, "prehash").subscribe().asStream().toList();

    System.out.println("\nXML document Generated XML Event Pre Hashes : \n" + xmlHashIds);
    System.out.println("\nJSON document Generated XML Event Pre Hashes : \n" + jsonHashIds);
    assertEquals(xmlHashIds, jsonHashIds);

    // Check if the response from Python Event Hash Generator is same as Java tool
    // final HttpRequest xmlRequest =
    // XML_REQUEST_BUILDER.POST(HttpRequest.BodyPublishers.ofString(new
    // String(getClass().getResourceAsStream("/WithFullCombinationOfFields.xml").readAllBytes(),
    // StandardCharsets.UTF_8))).build();
    // assertEquals(HttpClient.newHttpClient().send(xmlRequest,
    // HttpResponse.BodyHandlers.ofString()).body(), xmlHashIds.get(0));

    // Check if the response from Python Event Hash Generator is same as Java tool
    // final HttpRequest jsonRequest =
    // JSON_REQUEST_BUILDER.POST(HttpRequest.BodyPublishers.ofString(new
    // String(getClass().getResourceAsStream("/WithFullCombinationOfFields.json").readAllBytes(),
    // StandardCharsets.UTF_8))).build();
    // assertEquals(HttpClient.newHttpClient().send(jsonRequest,
    // HttpResponse.BodyHandlers.ofString()).body(), jsonHashIds.get(0));
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
}
