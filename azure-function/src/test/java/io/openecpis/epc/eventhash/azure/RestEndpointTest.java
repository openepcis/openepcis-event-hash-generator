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
        .body(getClass().getResourceAsStream("/biofresh-test-01.json"))
        .post("/api/documentHashIdGenerator")
        .then()
        .statusCode(RestResponse.StatusCode.OK);
  }
}
