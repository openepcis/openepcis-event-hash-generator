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

import io.openepcis.model.epcis.EPCISDocument;
import io.openepcis.model.rest.ProblemResponseBody;
import io.smallrye.mutiny.Multi;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBodySchema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(
    name = "Event Hash Generator",
    description = "Generate event hash for EPCIS XML or JSON/JSON-LD document or event list.")
@WebServlet(name = "EPCSISDocumentHashIdServlet", urlPatterns = "/api/generate/event-hash/document")
@Path("/api/generate/event-hash/document")
@Produces(MediaType.APPLICATION_JSON)
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class EPCSISDocumentHashIdServlet extends AbstractHashIdServlet {

  @Override
  @RequestBody(
      description = "EPCIS Document",
      required = true,
      content = {
        @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.OBJECT))
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
  @RequestBodySchema(EPCISDocument.class)
  @Parameter(
      name = "prehash",
      in = ParameterIn.QUERY,
      description = "Display " + "Pre-Hash String",
      schema =
          @Schema(
              description = "empty defaults to false",
              enumeration = {"true", "false"}))
  @Parameter(
      name = "beautifyPreHash",
      in = ParameterIn.QUERY,
      description = "Beautify Pre-Hash String",
      schema =
          @Schema(
              description = "empty defaults to false",
              enumeration = {"true", "false"}))
  @Parameter(
      name = "hashAlgorithm",
      in = ParameterIn.QUERY,
      description = "Hash Algorithm Type : sha-256, sha3-512, etc.",
      schema =
          @Schema(
              defaultValue = "sha-256",
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
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException {

    final Boolean prehash = Boolean.valueOf(req.getParameter("prehash"));
    final Boolean beautifyPreHash = Boolean.valueOf(req.getParameter("beautifyPreHash"));
    final List<String> hashAlgorithms =
        req.getParameter("hashAlgorithm") != null
            ? List.of(req.getParameterValues("hashAlgorithm"))
            : List.of("sha-256");
    final String contentType = req.getContentType();

    final List<String> hashParameters = getHashParameters(prehash, beautifyPreHash, hashAlgorithms);
    Multi<Map<String, String>> result =
        req.getContentType().equals("application/xml")
            ? eventHashGenerator.fromXml(
                req.getInputStream(), hashParameters.toArray(String[]::new))
            : eventHashGenerator.fromJson(
                req.getInputStream(), hashParameters.toArray(String[]::new));
    writeResult(resp, result);
  }
}
