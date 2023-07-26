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
package openepcis.epc.eventhash.generator.resource;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import io.openepcis.epc.eventhash.EventHashGenerator;
import io.openepcis.epc.eventhash.exception.EventHashException;
import io.openepcis.model.epcis.EPCISDocument;
import io.openepcis.model.epcis.EPCISEvent;
import io.openepcis.model.rest.ProblemResponseBody;
import io.smallrye.mutiny.Multi;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBodySchema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api")
@Tag(
    name = "Event Hash Generator",
    description = "Generate event hash for EPCIS XML or JSON/JSON-LD document or event list.")
public class EventHashGeneratorResource {

  @Inject
  ManagedExecutor managedExecutor;
  @Inject EventHashGenerator eventHashGenerator;
  @Inject JsonFactory jsonFactory;
  private static final String SHA_256 = "sha-256";

  // Method to convert the input XML/JSON EPCIS Document into Hash Ids based on the event
  // information present in them.
  @Path("/generate/event-hash/document")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON})
  @RequestBody(
      description = "Generate event hash for EPCIS 2.0 document in XML or JSON/JSON-LD format.")
  @POST
  @Operation(summary = "Generate event hash for EPCIS 2.0 document in XML or JSON/JSON-LD format.")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "OK: HashID generated successfully.",
            content =
                @Content(
                    example =
                        """
                                                    [
                                                     { "sha-256": "ni:///sha-256;995dc675f5bcf4300adc4c54a0a806371189b0cecdc214e47f0fb0947ec4e8cb?ver=CBV2.0" },
                                                     { "sha-256": "ni:///sha-256;0f539071b76afacd62bd8dfd103fa3645237cb31fd55ceb574f179d646a5fd08?ver=CBV2.0" }
                                                    ]
                                                     """,
                    schema = @Schema(type = SchemaType.ARRAY, implementation = String.class))),
        @APIResponse(
            responseCode = "400",
            description = "Bad Request: Input EPCIS document contain missing/invalid information.",
            content = @Content(schema = @Schema(implementation = ProblemResponseBody.class))),
        @APIResponse(
            responseCode = "401",
            description =
                "Unauthorized: Unable to generate Hash-ID as request contain missing/invalid authorization.",
            content = @Content(schema = @Schema(implementation = ProblemResponseBody.class))),
        @APIResponse(
            responseCode = "404",
            description =
                "Not Found: Unable to generate Hash-ID as the requested resource not found.",
            content = @Content(schema = @Schema(implementation = ProblemResponseBody.class))),
        @APIResponse(
            responseCode = "406",
            description =
                "Not Acceptable: Unable to generate Hash-ID as server cannot find content confirming request.",
            content = @Content(schema = @Schema(implementation = ProblemResponseBody.class))),
        @APIResponse(
            responseCode = "500",
            description =
                "Internal Server Error: Unable to generate Hash-ID document as server encountered problem.",
            content = @Content(schema = @Schema(implementation = ProblemResponseBody.class)))
      })
  public Multi<Map<String, String>> generateHashId(
      @HeaderParam("Content-Type") final String contentType,
      @RequestBodySchema(EPCISDocument.class) final InputStream inputDocumentStream,
      @Parameter(
              description = "Hash Algorithm Type : sha-256, sha3-512, etc.",
              schema =
                  @Schema(
                      description = "empty defaults to sha-256",
                      enumeration = {
                        "sha-1",
                        "sha-224",
                        "sha-256",
                        "sha-384",
                        "sha-512",
                        "sha3-224",
                        "sha3-256",
                        "sha3-384",
                        "sha3-512",
                        "md2",
                        "md5"
                      }))
          @QueryParam("hashAlgorithm")
          String hashAlgorithm,
      @Parameter(
              description = "Display " + "Pre-Hash String",
              schema =
                  @Schema(
                      description = "empty defaults to false",
                      enumeration = {"true", "false"}))
          @DefaultValue("false")
          @QueryParam("prehash")
          Boolean prehash,
      @Parameter(
              description = "Beautify Pre-Hash String",
              schema =
                  @Schema(
                      description = "empty defaults to false",
                      enumeration = {"true", "false"}))
          @DefaultValue("false")
          @QueryParam("beautifyPreHash")
          Boolean beautifyPreHash,
      @Parameter(
              description = "Ignore fields for Hash-ID generation",
              schema = @Schema(description = "empty defaults to no fields ignored"))
          @DefaultValue("")
          @QueryParam("ignoreFields")
          String ignoreFields)
      throws IOException {
    // List to store the parameters based on the user provided inputs.
    final List<String> hashParameters = new ArrayList<>();

    // If Pre-Hash string is requested then add the prehash string to the List
    if (Boolean.TRUE.equals(prehash)) {
      hashParameters.add("prehash");

      // If user has requested for beautification for prehash string then add beautification.
      if (beautifyPreHash != null && beautifyPreHash) {
        eventHashGenerator.prehashJoin("\\n");
      } else {
        eventHashGenerator.prehashJoin("");
      }
    }

    // If user has provided fields to ignore during hash generation then add them
    if (!StringUtils.isBlank(ignoreFields)) {
      eventHashGenerator.excludeFieldsInPreHash(ignoreFields);
    }

    // Add the Hash Algorithm type to the List.
    hashParameters.add(hashAlgorithm != null && !hashAlgorithm.isEmpty() ? hashAlgorithm : SHA_256);

    return (contentType.contains("application/xml")
            ? eventHashGenerator.fromXml(inputDocumentStream, hashParameters.toArray(String[]::new))
            : eventHashGenerator.fromJson(
                inputDocumentStream, hashParameters.toArray(String[]::new)))
        .runSubscriptionOn(managedExecutor);
  }

  // API end point for the single/List of EPCIS event in JSON format.
  @Path("/generate/event-hash/events")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON})
  @RequestBody(
      description = "Generate Hash-Ids for EPCIS events in XML/JSON format.",
      content =
          @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = EPCISEvent.class)))
  @POST
  @Operation(
      summary = "Generate event hash for list of EPCIS 2.0 events in XML or JSON/JSON-LD format.")
  @APIResponses(
      value = {
        @APIResponse(
            responseCode = "200",
            description = "OK: HashID generated successfully.",
            content =
                @Content(
                    example =
                        """
                                                    [
                                                     { "sha-256": "ni:///sha-256;995dc675f5bcf4300adc4c54a0a806371189b0cecdc214e47f0fb0947ec4e8cb?ver=CBV2.0" },
                                                     { "sha-256": "ni:///sha-256;0f539071b76afacd62bd8dfd103fa3645237cb31fd55ceb574f179d646a5fd08?ver=CBV2.0" }
                                                    ]
                                                     """,
                    schema = @Schema(type = SchemaType.ARRAY, implementation = String.class))),
        @APIResponse(
            responseCode = "400",
            description = "Bad Request: Input EPCIS events contain missing/invalid information.",
            content = @Content(schema = @Schema(implementation = ProblemResponseBody.class))),
        @APIResponse(
            responseCode = "401",
            description =
                "Unauthorized: Unable to generate Hash-ID as request contain missing/invalid authorization.",
            content = @Content(schema = @Schema(implementation = ProblemResponseBody.class))),
        @APIResponse(
            responseCode = "404",
            description =
                "Not Found: Unable to generate Hash-ID as the requested resource not found.",
            content = @Content(schema = @Schema(implementation = ProblemResponseBody.class))),
        @APIResponse(
            responseCode = "406",
            description =
                "Not Acceptable: Unable to generate Hash-ID as server cannot find content confirming request.",
            content = @Content(schema = @Schema(implementation = ProblemResponseBody.class))),
        @APIResponse(
            responseCode = "500",
            description =
                "Internal Server Error: Unable to generate Hash-ID as server encountered problem.",
            content = @Content(schema = @Schema(implementation = ProblemResponseBody.class)))
      })
  public Multi<Map<String, String>> generateEventHashIds(
      @HeaderParam("Content-Type") final String contentType,
      @RequestBodySchema(EPCISDocument.class) final InputStream inputDocumentStream,
      @Parameter(
              description = "Hash Algorithm Type : sha-256, sha3-512, etc.",
              schema =
                  @Schema(
                      description = "empty defaults to sha-256",
                      enumeration = {
                        "sha-1",
                        "sha-224",
                        "sha-256",
                        "sha-384",
                        "sha-512",
                        "sha3-224",
                        "sha3-256",
                        "sha3-384",
                        "sha3-512",
                        "md2",
                        "md5"
                      }))
          @QueryParam("hashAlgorithm")
          String hashAlgorithm,
      @Parameter(
              description = "Display Pre-Hash String",
              schema =
                  @Schema(
                      description = "empty defaults to false",
                      enumeration = {"true", "false"}))
          @DefaultValue("false")
          @QueryParam("prehash")
          Boolean prehash,
      @Parameter(
              description = "Beautify Pre-Hash String",
              schema =
                  @Schema(
                      description = "empty defaults to false",
                      enumeration = {"true", "false"}))
          @DefaultValue("false")
          @QueryParam("beautifyPreHash")
          Boolean beautifyPreHash)
      throws IOException {
    // List to store the parameters based on the user provided inputs.
    final List<String> hashParameters = new ArrayList<>();

    // If Pre-Hash string is requested then add the prehash string to the List
    if (Boolean.TRUE.equals(prehash)) {
      hashParameters.add("prehash");

      // If user has requested for beautification for prehash string then add beautification.
      if (beautifyPreHash != null && beautifyPreHash) {
        eventHashGenerator.prehashJoin("\\n");
      } else {
        eventHashGenerator.prehashJoin("");
      }
    }

    // Add the Hash Algorithm type to the List.
    hashParameters.add(hashAlgorithm != null && !hashAlgorithm.isEmpty() ? hashAlgorithm : SHA_256);

    return (contentType.contains("application/xml")
            ? eventHashGenerator.fromXml(inputDocumentStream, hashParameters.toArray(String[]::new))
            : eventHashGenerator.fromJson(
                generateJsonDocumentWrapper(inputDocumentStream),
                hashParameters.toArray(String[]::new)))
        .runSubscriptionOn(managedExecutor);
  }

  // Add the outer wrapper elements for the JSON eventList when array of EPCIS events is provided.
  private InputStream generateJsonDocumentWrapper(final InputStream inputEventList)
      throws IOException {
    final JsonParser jsonParser = jsonFactory.createParser(inputEventList);
    if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
      jsonParser.close();
      throw new IOException("Expecting input as JSON array");
    }
    final PipedOutputStream outTransform = new PipedOutputStream();
    final InputStream convertedDocument = new PipedInputStream(outTransform);
    managedExecutor.runAsync(
        () -> {
          try {
            final String documentHeader =
                String.format(
                    """
                                {
                                  "@context": [
                                    "https://ref.gs1.org/standards/epcis/2.0.0/epcis-context.jsonld"
                                  ],
                                  "type": "EPCISDocument",
                                  "schemaVersion": "2.0",
                                  "creationDate": "%s",
                                  "epcisBody": {
                                    "eventList": [
                                """,
                    Instant.now().truncatedTo(ChronoUnit.MILLIS).toString());
            outTransform.write(documentHeader.getBytes(StandardCharsets.UTF_8));
            outTransform.flush();
            int eventIndex = 0;
            while (jsonParser.nextToken() != null) {
              if (jsonParser.currentToken() == JsonToken.START_OBJECT) {
                final JsonNode node = jsonParser.readValueAsTree();
                if (eventIndex++ > 0) {
                  outTransform.write(",\n".getBytes(StandardCharsets.UTF_8));
                }
                outTransform.write(node.toPrettyString().getBytes(StandardCharsets.UTF_8));
                outTransform.flush();
              }
            }
            outTransform.write(
                new String(
                        """
                                    ]
                                  }
                                }
                                """)
                    .getBytes(StandardCharsets.UTF_8));
            outTransform.flush();
            outTransform.close();
          } catch (Exception ex) {
            try {
              outTransform.write(
                  ("Exception occurred during the creation of wrapper document for eventList : "
                          + ex.getMessage())
                      .getBytes(StandardCharsets.UTF_8));
              outTransform.close();
            } catch (Exception ignore) {
              // ignored
            }
            throw new EventHashException(
                "Exception occurred during the creation of wrapper document for eventList : "
                    + ex.getMessage()
                    + ex);
          } finally {
            try {
              inputEventList.close();
            } catch (Exception ignore) {
              // ignored
            }
          }
        });
    return convertedDocument;
  }
}
