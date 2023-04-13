package io.openecpis.epc.eventhash.generator.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

public class CompareApiResultsTest {

  private String documentEventHashAPI;
  private String eventListEventHashAPI;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeSuite
  public void beforeEachTestMethod() {
    int port =
        ConfigProviderResolver.instance()
            .getConfig()
            .getOptionalValue("quarkus.http.port", Integer.class)
            .orElse(8080);

    final String basePath = "http://localhost:" + port;
    documentEventHashAPI = basePath + "/api/generate/event-hash/document";
    eventListEventHashAPI = basePath + "/api/generate/event-hash/events";
  }

  @DataProvider(name = "documentTestData")
  public Object[][] documentTestData() {
    return new Object[][] {
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
  }

  @DataProvider(name = "eventTestData")
  public Object[][] eventsTestData() {
    return new Object[][] {
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
  }

  // Compare Event Hash generated for XML and JSON document.
  // Parameterized test to compare event hash for XML and JSON documents
  @org.testng.annotations.Test(dataProvider = "documentTestData")
  public void compareDocumentEventHashTest(final String xmlFilePath, final String jsonFilePath)
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
            .post(documentEventHashAPI);

    final Response jsonResponse =
        given()
            .contentType(ContentType.JSON)
            .body(getClass().getClassLoader().getResourceAsStream(jsonFilePath))
            .when()
            .post(documentEventHashAPI);

    // Compare response bodies
    xmlResponse.then().statusCode(200);
    jsonResponse.then().statusCode(200);
    xmlResponse.then().body(equalTo(jsonResponse.getBody().asString()));
  }

  // Compare Event Hash generated for XML and JSON document.
  // Parameterized test to compare event hash for XML and JSON documents
  @org.testng.annotations.Test(dataProvider = "eventTestData")
  public void compareEventsListEventHashTest(final String xmlFilePath, final String jsonFilePath)
      throws IOException {
    final Response xmlResponse =
        given()
            .contentType(ContentType.XML)
            .body(
                IOUtils.toString(
                    Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream(xmlFilePath)),
                    StandardCharsets.UTF_8))
            .when()
            .post(eventListEventHashAPI);

    final ArrayNode jsonArray = objectMapper.createArrayNode();
    final ObjectNode objectNode =
        (ObjectNode)
            objectMapper.readTree(getClass().getClassLoader().getResourceAsStream(jsonFilePath));
    jsonArray.add(objectNode);

    final Response jsonResponse =
        given().contentType(ContentType.JSON).body(jsonArray).when().post(eventListEventHashAPI);

    // Compare response bodies
    xmlResponse.then().statusCode(200);
    jsonResponse.then().statusCode(200);
    xmlResponse.then().body(equalTo(jsonResponse.getBody().asString()));
  }
}
