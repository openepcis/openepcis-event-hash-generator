/*
 * Copyright 2022-2023 benelog GmbH & Co. KG
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
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import openepcis.epc.eventhash.generator.resource.EventHashGeneratorResource;
import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class RestEndpointTest {

  @TestHTTPEndpoint(EventHashGeneratorResource.class)
  @TestHTTPResource
  URL resourceUrl;

  private String basePath() {
    return resourceUrl.toString();
  }

  private String documentEventHashAPI() {
    return basePath() + "/generate/event-hash/document";
  }
  @Test
  public void swaggerUITest() {
    RestAssured.when()
        .get(basePath() + "/../q/swagger-ui/index.html")
        .then()
        .statusCode(RestResponse.StatusCode.OK);
  }

  @Test
  public void documentsTest() {
    RestAssured.given()
        .contentType("application/json")
        .accept("application/json")
        .body(getClass().getResourceAsStream("/SensorDataExample.json"))
        .post(basePath() + "/generate/event-hash/document")
        .then()
        .statusCode(RestResponse.StatusCode.OK);
  }

  // Status code test for JSON input
  @Test
  public void generateJsonDocumentEventHashStatusCodeTest() {
    given()
        .contentType(ContentType.JSON)
        .body(
            getClass()
                .getClassLoader()
                .getResourceAsStream("2.0/EPCIS/JSON/Capture/Documents/AggregationEvent.json"))
        .when()
        .post(documentEventHashAPI())
        .then()
        .statusCode(200);
  }

  // Invalid Request type for JSON input
  @Test
  public void generateJsonEventHashInvalidRequestTypeTest() {
    final Response jsonResponse =
        given()
            .contentType(ContentType.XML)
            .body(
                getClass()
                    .getClassLoader()
                    .getResourceAsStream("2.0/EPCIS/JSON/Capture/Documents/AggregationEvent.json"))
            .when()
            .post(documentEventHashAPI());

    assertEquals(415, jsonResponse.statusCode());
  }

  // Invalid Request type for XML input
  @Test
  public void generateXmlEventHashInvalidRequestTypeTest() throws IOException {
    final Response xmlResponse =
        given()
            .contentType(ContentType.JSON)
            .body(
                IOUtils.toString(
                    Objects.requireNonNull(
                        getClass()
                            .getClassLoader()
                            .getResourceAsStream(
                                "2.0/EPCIS/XML/Capture/Documents/AggregationEvent.xml")),
                    StandardCharsets.UTF_8))
            .when()
            .post(documentEventHashAPI());

    assertEquals(415, xmlResponse.statusCode());
    assertEquals("JsonParseException", xmlResponse.jsonPath().get("type"));
  }

  // Status code test for XML input
  @Test
  public void generateXmlEventHashStatusCodeTest() throws IOException {
    given()
        .contentType(ContentType.XML)
        .body(
            IOUtils.toString(
                Objects.requireNonNull(
                    getClass()
                        .getClassLoader()
                        .getResourceAsStream(
                            "2.0/EPCIS/XML/Capture/Documents/AggregationEvent.xml")),
                StandardCharsets.UTF_8))
        .when()
        .post(documentEventHashAPI())
        .then()
        .statusCode(200);
  }
}
