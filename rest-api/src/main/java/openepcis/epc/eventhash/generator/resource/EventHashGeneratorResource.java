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
package openepcis.epc.eventhash.generator.resource;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.openepcis.epc.eventhash.EventHashGenerator;
import io.openepcis.model.epcis.EPCISDocument;
import io.openepcis.model.epcis.EPCISEvent;
import io.openepcis.model.rest.ProblemResponseBody;
import io.smallrye.mutiny.Multi;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
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
    name = "Hash-Id Generator",
    description = "Generate Hash-Ids for EPCIS XML/JSON document/events.")
public class EventHashGeneratorResource {

  @Inject JsonFactory jsonfactory;
  private static final String SHA_256 = "sha-256";

  // Method to convert the input XML/JSON EPCIS Document into Hash Ids based on the event
  // information present in them.
  @Operation(summary = "Generate Hash-Ids for EPCIS document in XML/JSON format.")
  @POST
  @Path("/documentHashIdGenerator")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON})
  @RequestBody(description = "Generate Hash-Ids for standard EPCIS document in XML/JSON format.")
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
                           "ni:///sha-256;995dc675f5bcf4300adc4c54a0a806371189b0cecdc214e47f0fb0947ec4e8cb?ver=CBV2.0",
                           "ni:///sha-256;0f539071b76afacd62bd8dfd103fa3645237cb31fd55ceb574f179d646a5fd08?ver=CBV2.0"
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
                      enumeration = {"True", "False"}))
          @DefaultValue("false")
          @QueryParam("prehash")
          Boolean prehash,
      @Parameter(
              description = "Beautify Pre-Hash String",
              schema =
                  @Schema(
                      description = "empty defaults to false",
                      enumeration = {"True", "False"}))
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
        EventHashGenerator.prehashJoin("\\n");
      } else {
        EventHashGenerator.prehashJoin("");
      }
    }

    // Add the Hash Algorithm type to the List.
    hashParameters.add(hashAlgorithm != null && !hashAlgorithm.isEmpty() ? hashAlgorithm : SHA_256);

    return contentType.equals("application/xml")
        ? EventHashGenerator.fromXml(inputDocumentStream, hashParameters.toArray(String[]::new))
        : EventHashGenerator.fromJson(inputDocumentStream, hashParameters.toArray(String[]::new));
  }

  // API end point for the single/List of EPCIS event in JSON format.
  @Operation(summary = "Generate Hash-Ids for list of EPCIS events in XML/JSON format.")
  @POST
  @Path("/eventHashIdGenerator")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON})
  @RequestBody(
      description = "Generate Hash-Ids for EPCIS events in XML/JSON format.",
      content =
          @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = EPCISEvent.class)))
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
                           "ni:///sha-256;995dc675f5bcf4300adc4c54a0a806371189b0cecdc214e47f0fb0947ec4e8cb?ver=CBV2.0",
                           "ni:///sha-256;0f539071b76afacd62bd8dfd103fa3645237cb31fd55ceb574f179d646a5fd08?ver=CBV2.0"
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
                      enumeration = {"True", "False"}))
          @DefaultValue("false")
          @QueryParam("prehash")
          Boolean prehash,
      @Parameter(
              description = "Beautify Pre-Hash String",
              schema =
                  @Schema(
                      description = "empty defaults to false",
                      enumeration = {"True", "False"}))
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
        EventHashGenerator.prehashJoin("\\n");
      } else {
        EventHashGenerator.prehashJoin("");
      }
    }

    // Add the Hash Algorithm type to the List.
    hashParameters.add(hashAlgorithm != null && !hashAlgorithm.isEmpty() ? hashAlgorithm : SHA_256);

    return contentType.equals("application/xml")
        ? EventHashGenerator.fromXml(inputDocumentStream, hashParameters.toArray(String[]::new))
        : EventHashGenerator.fromJson(
            generateJsonDocumentWrapper(inputDocumentStream),
            hashParameters.toArray(String[]::new));
  }

  // Add the outer wrapper elements for the JSON eventList when array of EPCIS events is provided.
  private InputStream generateJsonDocumentWrapper(final InputStream inputEventList)
      throws IOException {
    final StringWriter jsonDocumentWriter = new StringWriter();
    try (JsonGenerator jsonGenerator = jsonfactory.createGenerator(jsonDocumentWriter)) {
      jsonfactory.createGenerator(jsonDocumentWriter);
      jsonGenerator.writeStartObject();
      jsonGenerator.writeStringField("type", "EPCISDocument");
      jsonGenerator.writeStringField("schemaVersion", "2.0");
      jsonGenerator.writeStringField("creationDate", Instant.now().toString());
      jsonGenerator.writeObjectFieldStart("epcisBody");
      jsonGenerator.writeFieldName("eventList");
      jsonGenerator.writeRawValue(IOUtils.toString(inputEventList, StandardCharsets.UTF_8));
      jsonGenerator.writeEndObject();
      jsonGenerator.writeEndObject();
      jsonGenerator.flush();
    } catch (IOException ex) {
      throw new IOException();
    }
    return IOUtils.toInputStream(jsonDocumentWriter.toString(), StandardCharsets.UTF_8);
  }
}