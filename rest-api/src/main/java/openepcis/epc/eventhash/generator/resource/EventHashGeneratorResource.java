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
import io.openepcis.constants.CBVVersion;
import io.openepcis.epc.eventhash.DocumentWrapperSupport;
import io.openepcis.epc.eventhash.EventHashGenerator;
import io.openepcis.model.epcis.EPCISDocument;
import io.openepcis.model.epcis.EPCISEvent;
import io.openepcis.model.rest.ProblemResponseBody;
import io.openepcis.resources.oas.EPCISExampleOASFilter;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBodySchema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("/api")
@Tag(
    name = "Event Hash Generator",
    description = "Generate event hash for EPCIS XML or JSON/JSON-LD document or event list.")
public class EventHashGeneratorResource {

  @Inject
  ManagedExecutor managedExecutor;
  @Inject EventHashGenerator eventHashGenerator;
  @Inject JsonFactory jsonFactory;

  @Inject
  DocumentWrapperSupport documentWrapperSupport;
  private static final String SHA_256 = "sha-256";

  // Method to convert the input XML/JSON EPCIS Document into Hash Ids based on the event
  // information present in them.
  @Path("/generate/event-hash/document")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON})
  @RequestBody(
          description =
                  "EPCIS 2.0 document in XML or JSON/JSON-LD format.",
          content = {
                  @Content(
                          mediaType = MediaType.APPLICATION_JSON,
                          schema = @Schema(implementation = EPCISDocument.class),
                          examples = {
                                  @ExampleObject(
                                          name = "EPCIS 2.0 JSON document",
                                          ref = "jsonDocument",
                                          description = "Example EPCIS 2.0 document in JSON format.")
                          }),
                  @Content(
                          mediaType = MediaType.APPLICATION_XML,
                          schema = @Schema(implementation = EPCISDocument.class),
                          examples = {
                                  @ExampleObject(
                                          name = "EPCIS 2.0 XML document",
                                          ref = "xmlDocument",
                                          description = "Example EPCIS 2.0 document in XML format.")
                          }),
                  @Content(
                          mediaType = "application/ld+json",
                          schema = @Schema(implementation = EPCISDocument.class),
                          examples = {
                                  @ExampleObject(
                                          name = "EPCIS 2.0 JSON document",
                                          ref = "jsonDocument",
                                          description = "Example EPCIS 2.0 document in JSON format.")
                          })
          })
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
                        "sha-224",
                        "sha-256",
                        "sha-384",
                        "sha-512",
                        "sha3-224",
                        "sha3-256",
                        "sha3-512"
                      }))
          @DefaultValue("sha-256")
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
          String ignoreFields,
      @Parameter(
              description = "CBV version based on which Pre-Hash String and Hash-Id needs to be generated",
              schema =
              @Schema(
                      description = "empty defaults to CBV version 2.0.0",
                      enumeration = {"2.0.0", "2.1.0"}))
      @DefaultValue("2.0.0")
      @QueryParam("cbvVersion")
      String cbvVersion)
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

    // Based on CBV version provided set the respective cbv version, default to 2.0.0
    final CBVVersion targetCbvVersion = CBVVersion.VERSION_2_1_0.equals(CBVVersion.toCbvVersion(cbvVersion)) ? CBVVersion.VERSION_2_1_0 : CBVVersion.VERSION_2_0_0;

    return (contentType.contains("application/xml")
            ? eventHashGenerator.mapCbvVersion(targetCbvVersion).fromXml(inputDocumentStream, hashParameters.toArray(String[]::new))
            : eventHashGenerator.mapCbvVersion(targetCbvVersion).fromJson(
                inputDocumentStream, hashParameters.toArray(String[]::new)))
        .runSubscriptionOn(managedExecutor);
  }

  // API end point for the single/List of EPCIS event in JSON format.
  @Path("/generate/event-hash/events")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON})
  @RequestBody(
          description =
                  "List of EPCIS 2.0 events JSON/JSON-LD format or EPCIS 2.0 XML event.",
          content = {
                  @Content(
                          mediaType = MediaType.APPLICATION_JSON,
                          schema = @Schema(type = SchemaType.ARRAY, implementation = EPCISEvent.class),
                          examples = {
                                  @ExampleObject(
                                          name = "EPCIS 2.0 JSON event list",
                                          ref = "jsonEventsList",
                                          description = "Example EPCIS 2.0 document in JSON format.")
                          }),
                  @Content(
                          mediaType = MediaType.APPLICATION_XML,
                          schema = @Schema(implementation = EPCISEvent.class),
                          examples = {
                                  @ExampleObject(
                                          name = "EPCIS 2.0 XML event",
                                          ref = EPCISExampleOASFilter.EXAMPLE_2_0_0_XML_OBJECT_EVENT,
                                          description = "Example EPCIS 2.0 event in XML format.")
                          }),
                  @Content(
                          mediaType = "application/ld+json",
                          schema = @Schema(type = SchemaType.ARRAY, implementation = EPCISEvent.class),
                          examples = {
                                  @ExampleObject(
                                          name = "EPCIS 2.0 JSON event list",
                                          ref = "jsonEventsList",
                                          description = "Example EPCIS 2.0 document in JSON format.")
                          })
          })
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
                        "sha-224",
                        "sha-256",
                        "sha-384",
                        "sha-512",
                        "sha3-224",
                        "sha3-256",
                        "sha3-512"
                      }))
          @DefaultValue("sha-256")
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
          Boolean beautifyPreHash,
      @Parameter(
              description = "CBV version based on which Pre-Hash String and Hash-Id needs to be generated",
              schema =
              @Schema(
                      description = "empty defaults to CBV version 2.0.0",
                      enumeration = {"2.0.0", "2.1.0"}))
      @DefaultValue("2.0.0")
      @QueryParam("cbvVersion")
      String cbvVersion)
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

    // Based on CBV version provided set the respective cbv version, default to 2.0.0
    final CBVVersion targetCbvVersion = CBVVersion.VERSION_2_1_0.equals(CBVVersion.toCbvVersion(cbvVersion)) ? CBVVersion.VERSION_2_1_0 : CBVVersion.VERSION_2_0_0;

    return (contentType.contains("application/xml")
            ? eventHashGenerator.mapCbvVersion(targetCbvVersion).fromXml(inputDocumentStream, hashParameters.toArray(String[]::new))
            : eventHashGenerator.mapCbvVersion(targetCbvVersion).fromJson(
                documentWrapperSupport.generateJsonDocumentWrapper(inputDocumentStream),
                hashParameters.toArray(String[]::new)))
        .runSubscriptionOn(managedExecutor);
  }

}
