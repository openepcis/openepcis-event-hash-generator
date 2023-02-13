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
package io.openecpis.epc.eventhash.azure;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class RestEndpointTest {
  // NOTE: RestAssured is aware of the application.properties quarkus.http.root-path switch

  @Test
  public void testSwaggerUI() {
    RestAssured.when()
        .get("/q/swagger-ui/index.html")
        .then()
        .statusCode(RestResponse.StatusCode.OK);
  }

  @Test
  public void testDocuments() {
    RestAssured.given()
        .contentType("application/json")
        .accept("application/json")
        .body(getClass().getResourceAsStream("/SensorDataExample.json"))
        .post("/api/generate/event-hash/document")
        .then()
        .statusCode(RestResponse.StatusCode.OK);
  }
}
