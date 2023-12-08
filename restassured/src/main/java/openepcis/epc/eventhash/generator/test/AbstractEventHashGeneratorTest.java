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
package openepcis.epc.eventhash.generator.test;

import io.openepcis.resources.util.Commons;
import io.openepcis.resources.util.ResourceFinder;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

@QuarkusTest
@Slf4j
public abstract class AbstractEventHashGeneratorTest {

    static final List<String> INVALID_HASH_DOCS = List.of(
            "ObjectEvent_all_possible_fields",
            "AggregationEvent_with_sensorData",
            "TransformationEvent_all_possible_fields");

    static final List<String> INVALID_HASH_EVENTS = List.of(
            "ObjectEvent");

    abstract protected String documentApi();

    abstract protected String eventsApi();

    @Test
    public void invalidContentTypeTest() {
        Stream.of(documentApi(), eventsApi()).forEach(url -> {
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(Commons.getInputStream("2.0/EPCIS/XML/Capture/Documents/AggregationEvent.xml"))
                    .when()
                    .post(url)
                    .then()
                    .assertThat()
                    .statusCode(415)
                    .body("title", Matchers.equalTo("Unsupported Media Type"));
        });
    }

    @Test
    public void hashGeneratorDocumentComparisonTest() {
        List<URL> jsonCaptureFiles = ResourceFinder.searchResource("2.0", "json", "capture/documents", null);
        List<URL> xmlCaptureFiles = ResourceFinder.searchResource("2.0", "xml", "capture/documents", null);
        jsonCaptureFiles.stream().forEach(url -> {
            try {
                final Response jsonResponse = RestAssured.given()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(ContentType.JSON)
                        .body(url.openStream())
                        .post(documentApi());
                jsonResponse.then()
                        .assertThat()
                        .statusCode(200);
                final String jsonString = jsonResponse.body().asString();
                final URL xmlUrl = ResourceFinder.matching(url, xmlCaptureFiles);
                if (xmlUrl != null) {
                    log.debug("testing XML " + xmlUrl.getFile().substring(xmlUrl.getFile().lastIndexOf("/")));
                    final Response xmlResponse = RestAssured.given()
                            .contentType(MediaType.APPLICATION_XML)
                            .accept(ContentType.JSON)
                            .body(IOUtils.toString(xmlUrl.openStream(), StandardCharsets.UTF_8))
                            .post(documentApi());
                    xmlResponse.then()
                            .assertThat()
                            .statusCode(200);
                    if (!INVALID_HASH_DOCS.stream().anyMatch(s -> xmlUrl.getFile().contains(s))) {
                        Assertions.assertEquals(jsonResponse.body().asString(), xmlResponse.body().asString());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


    @Test
    public void hashGeneratorEventsComparisonTest() {
        List<URL> jsonCaptureFiles = ResourceFinder.searchResource("2.0", "json", "capture/events", null);
        List<URL> xmlCaptureFiles = ResourceFinder.searchResource("2.0", "xml", "capture/events", null);
        jsonCaptureFiles.stream().forEach(url -> {
            try {
                final Response jsonResponse = RestAssured.given()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(ContentType.JSON)
                        .body("["+IOUtils.toString(url.openStream(), StandardCharsets.UTF_8)+"]")
                        .post(eventsApi());
                jsonResponse.then()
                        .assertThat()
                        .statusCode(200);
                final String jsonString = jsonResponse.body().asString();
                final URL xmlUrl = ResourceFinder.matching(url, xmlCaptureFiles);
                if (xmlUrl != null) {
                    log.debug("testing XML " + xmlUrl.getFile().substring(xmlUrl.getFile().lastIndexOf("/")));
                    final Response xmlResponse = RestAssured.given()
                            .contentType(MediaType.APPLICATION_XML)
                            .accept(ContentType.JSON)
                            .body(IOUtils.toString(xmlUrl.openStream(), StandardCharsets.UTF_8))
                            .post(documentApi());
                    xmlResponse.then()
                            .assertThat()
                            .statusCode(200);
                    if (!INVALID_HASH_EVENTS.stream().anyMatch(s -> xmlUrl.getFile().contains(s))) {
                        Assertions.assertEquals(jsonResponse.body().asString(), xmlResponse.body().asString());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
