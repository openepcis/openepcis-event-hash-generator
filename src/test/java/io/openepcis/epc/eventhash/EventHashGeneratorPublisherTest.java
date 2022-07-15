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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class EventHashGeneratorPublisherTest {

  private static final String PYTHON_TOOL_API = "https://event-hash-generator.openepcis.io/hash";
  private static final HttpRequest.Builder XML_REQUEST_BUILDER =
      HttpRequest.newBuilder(URI.create(PYTHON_TOOL_API)).header("content-type", "application/xml");
  private static final HttpRequest.Builder JSON_REQUEST_BUILDER =
      HttpRequest.newBuilder(URI.create(PYTHON_TOOL_API))
          .header("content-type", "application/json");

  // General test to fix bugs or necessary code modification for XML document.
  @Test
  public void xmlHashGeneratorTest()
      throws SAXException, ParserConfigurationException, IOException {
    final EventHashGenerator eventHashGenerator = new EventHashGenerator();
    final InputStream xmlStream = getClass().getResourceAsStream("/XmlEpcisEvents.xml");
    final List<String> eventHashIds =
        eventHashGenerator
            .xmlDocumentHashIdGenerator(xmlStream, "sha-256")
            .subscribe()
            .asStream()
            .toList();
    assertEquals(1, eventHashIds.size());
    System.out.println("\nXML document Generated Event Hash Ids : \n" + eventHashIds);
  }

  // General test to fix bugs or necessary code modification for JSON document.
  @Test
  public void jsonHashGeneratorTest() throws Exception {
    final EventHashGenerator eventHashGenerator = new EventHashGenerator();
    final InputStream jsonStream = getClass().getResourceAsStream("/JsonEpcisEvents.json");
    final List<String> eventHashIds =
        eventHashGenerator
            .jsonDocumentHashIdGenerator(jsonStream, "sha3-256")
            .subscribe()
            .asStream()
            .toList();
    assertEquals(1, eventHashIds.size());
    System.out.println("\nJSON/JSON-LD document Generated Event Hash Ids : \n" + eventHashIds);
  }

  // Test to ensure the pre-hash string is generated correctly for simple event.
  @Test
  public void withSimpleSingleEventTest()
      throws SAXException, ParserConfigurationException, IOException, InterruptedException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.
    final EventHashGenerator eventHashGenerator = new EventHashGenerator();

    final InputStream xmlStream = getClass().getResourceAsStream("/SingleEvent.xml");
    final InputStream jsonStream = getClass().getResourceAsStream("/SingleEvent.json");

    final List<String> xmlHashIds =
        eventHashGenerator
            .xmlDocumentHashIdGenerator(xmlStream, "sha-256")
            .subscribe()
            .asStream()
            .toList();
    final List<String> jsonHashIds =
        eventHashGenerator
            .jsonDocumentHashIdGenerator(jsonStream, "sha-256")
            .subscribe()
            .asStream()
            .toList();

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
  public void withErrorDeclarationEventTest()
      throws SAXException, ParserConfigurationException, IOException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.
    final EventHashGenerator eventHashGenerator = new EventHashGenerator();

    final InputStream xmlStream = getClass().getResourceAsStream("/WithErrorDeclaration.xml");
    final InputStream jsonStream = getClass().getResourceAsStream("/WithErrorDeclaration.json");

    final List<String> xmlHashIds =
        eventHashGenerator
            .xmlDocumentHashIdGenerator(xmlStream, "sha-256")
            .subscribe()
            .asStream()
            .toList();
    final List<String> jsonHashIds =
        eventHashGenerator
            .jsonDocumentHashIdGenerator(jsonStream, "sha-256")
            .subscribe()
            .asStream()
            .toList();

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
  public void withFullCombinationFieldsTest()
      throws SAXException, ParserConfigurationException, IOException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.
    final EventHashGenerator eventHashGenerator = new EventHashGenerator();

    final InputStream xmlStream =
        getClass().getResourceAsStream("/WithFullCombinationOfFields.xml");
    final InputStream jsonStream =
        getClass().getResourceAsStream("/WithFullCombinationOfFields.json");

    final List<String> xmlHashIds =
        eventHashGenerator
            .xmlDocumentHashIdGenerator(xmlStream, "sha-256")
            .subscribe()
            .asStream()
            .toList();
    final List<String> jsonHashIds =
        eventHashGenerator
            .jsonDocumentHashIdGenerator(jsonStream, "sha-256")
            .subscribe()
            .asStream()
            .toList();

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
  public void withJumbledOrderFieldsTest()
      throws SAXException, ParserConfigurationException, IOException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.
    final EventHashGenerator eventHashGenerator = new EventHashGenerator();

    final InputStream xmlStream = getClass().getResourceAsStream("/withJumbledFieldsOrder.xml");
    final InputStream jsonStream = getClass().getResourceAsStream("/withJumbledFieldsOrder.json");

    final List<String> xmlHashIds =
        eventHashGenerator
            .xmlDocumentHashIdGenerator(xmlStream, "sha-256")
            .subscribe()
            .asStream()
            .toList();
    final List<String> jsonHashIds =
        eventHashGenerator
            .jsonDocumentHashIdGenerator(jsonStream, "sha-256")
            .subscribe()
            .asStream()
            .toList();

    assertEquals(xmlHashIds, jsonHashIds);
  }

  // Test to ensure order of User Extensions are always lexicographical order.
  @Test
  public void withUserExtensions() throws SAXException, ParserConfigurationException, IOException {
    // For same event in XML & JSON format check if the generated Hash-IDs match.
    final EventHashGenerator eventHashGenerator = new EventHashGenerator();

    final InputStream xmlStream = getClass().getResourceAsStream("/withUserExtensions.xml");
    final InputStream jsonStream = getClass().getResourceAsStream("/withUserExtensions.json");

    final List<String> xmlHashIds =
        eventHashGenerator
            .xmlDocumentHashIdGenerator(xmlStream, "sha-256")
            .subscribe()
            .asStream()
            .toList();
    final List<String> jsonHashIds =
        eventHashGenerator
            .jsonDocumentHashIdGenerator(jsonStream, "sha-256")
            .subscribe()
            .asStream()
            .toList();

    assertEquals(xmlHashIds, jsonHashIds);
  }

  // Test to ensure different combination of events in single EPCIS document
  @Test
  public void withCombinationOfEvents()
      throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException,
          IOException {
    final EventHashGenerator eventHashGenerator = new EventHashGenerator();

    final InputStream xmlStream = getClass().getResourceAsStream("/EventCombination.xml");
    final InputStream jsonStream = getClass().getResourceAsStream("/EventCombination.json");

    final List<String> xmlHashIds =
        eventHashGenerator
            .xmlDocumentHashIdGenerator(xmlStream, "sha-512")
            .subscribe()
            .asStream()
            .toList();
    final List<String> jsonHashIds =
        eventHashGenerator
            .jsonDocumentHashIdGenerator(jsonStream, "sha-512")
            .subscribe()
            .asStream()
            .toList();

    assertEquals(2, xmlHashIds.size());
    assertEquals(2, jsonHashIds.size());
    assertEquals(xmlHashIds, jsonHashIds);
  }
}
