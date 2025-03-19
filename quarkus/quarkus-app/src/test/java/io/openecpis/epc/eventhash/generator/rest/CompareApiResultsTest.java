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
package io.openecpis.epc.eventhash.generator.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openepcis.eventhash.generator.resource.EventHashGeneratorResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CompareApiResultsTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @TestHTTPEndpoint(EventHashGeneratorResource.class)
  @TestHTTPResource
  URL resourceUrl;

  private String basePath() {
    return resourceUrl.toString();
  }

  private String documentEventHashAPI() {
    return basePath() + "/generate/event-hash/document";
  }

  private String eventListEventHashAPI() {
    return basePath() + "/generate/event-hash/events";
  }

  private static final String[][] DOCUMENT_TEST_DATA =
      new String[][] {
        {
          "2.0/EPCIS/XML/Capture/Documents/AggregationEvent.xml",
          "2.0/EPCIS/JSON/Capture/Documents/AggregationEvent.json"
        },
        {
          "2.0/EPCIS/XML/Capture/Documents/Combination_of_different_event.xml",
          "2.0/EPCIS/JSON/Capture/Documents/Combination_of_different_event.json"
        },
        {
          "2.0/EPCIS/XML/Capture/Documents/SensorData_with_combined_events.xml",
          "2.0/EPCIS/JSON/Capture/Documents/SensorData_with_combined_events.json"
        },
        {
          "2.0/EPCIS/XML/Capture/Documents/TransformationEvent.xml",
          "2.0/EPCIS/JSON/Capture/Documents/TransformationEvent.json"
        },
        {
          "2.0/EPCIS/XML/Capture/Documents/Namespaces_at_different_level.xml",
          "2.0/EPCIS/JSON/Capture/Documents/Namespaces_at_different_level.json"
        }
      };

  private static final String[][] EVENTS_TEST_DATA =
      new String[][] {
        {
          "2.0/EPCIS/XML/Capture/Events/AggregationEvent.xml",
          "2.0/EPCIS/JSON/Capture/Events/AggregationEvent.json"
        },
        {
          "2.0/EPCIS/XML/Capture/Events/AssociationEvent.xml",
          "2.0/EPCIS/JSON/Capture/Events/AssociationEvent.json"
        },
        {
          "2.0/EPCIS/XML/Capture/Events/TransformationEvent.xml",
          "2.0/EPCIS/JSON/Capture/Events/TransformationEvent.json"
        }
      };

  @Test
  void testDocuments() throws IOException {
    for (String[] doc : Arrays.asList(DOCUMENT_TEST_DATA)) {
      compareDocumentEventHashTest(doc[0], doc[1]);
    }
  }

  @Test
  void testEvents() throws IOException {
    for (String[] doc : Arrays.asList(EVENTS_TEST_DATA)) {
      compareEventsListEventHashTest(doc[0], doc[1]);
    }
  }

  // Parameterized test to compare event hash for XML and JSON EPCIS documents
  private void compareDocumentEventHashTest(final String xmlFilePath, final String jsonFilePath)
      throws IOException {
    final Response xmlResponse =
        given()
            .header("Content-Type", "application/xml")
            .body(
                IOUtils.toString(
                    Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream(xmlFilePath)),
                    StandardCharsets.UTF_8))
            .when()
            .post(documentEventHashAPI());

    final Response jsonResponse =
        given()
            .contentType(ContentType.JSON)
            .body(getClass().getClassLoader().getResourceAsStream(jsonFilePath))
            .when()
            .post(documentEventHashAPI());

    // Compare response bodies
    xmlResponse.then().statusCode(200);
    jsonResponse.then().statusCode(200);
    xmlResponse.then().body(equalTo(jsonResponse.getBody().asString()));
  }

  // Parameterized test to compare event hash for XML and JSON EPCIS events
  private void compareEventsListEventHashTest(final String xmlFilePath, final String jsonFilePath)
      throws IOException {
    final Response xmlResponse =
        given()
            .contentType(ContentType.XML)
            .accept(ContentType.JSON)
            .body(
                IOUtils.toString(
                    Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream(xmlFilePath)),
                    StandardCharsets.UTF_8))
            .when()
            .post(eventListEventHashAPI());

    // Add the wrapper array for EPCIS events
    final ArrayNode jsonArray = objectMapper.createArrayNode();
    final ObjectNode objectNode =
        (ObjectNode)
            objectMapper.readTree(getClass().getClassLoader().getResourceAsStream(jsonFilePath));
    jsonArray.add(objectNode);

    final Response jsonResponse =
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(jsonArray)
            .when()
            .post(eventListEventHashAPI());

    // Compare response bodies
    xmlResponse.then().statusCode(200);
    jsonResponse.then().statusCode(200);
    xmlResponse.then().body(equalTo(jsonResponse.getBody().asString()));
  }
}
