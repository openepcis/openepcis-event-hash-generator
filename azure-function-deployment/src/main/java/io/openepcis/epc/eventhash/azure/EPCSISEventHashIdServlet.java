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
package io.openepcis.epc.eventhash.azure;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.openepcis.epc.eventhash.exception.EventHashException;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.context.ManagedExecutor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@WebServlet(name = "EPCSISEventHashIdServlet", urlPatterns = "/api/generate/event-hash/events")
public class EPCSISEventHashIdServlet extends AbstractHashIdServlet {

  @Inject JsonFactory jsonFactory;

  @Inject ManagedExecutor managedExecutor;

  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException {

    final Boolean prehash = Boolean.valueOf(req.getParameter("prehash"));
    final Boolean beautifyPreHash = Boolean.valueOf(req.getParameter("beautifyPreHash"));
    final List<String> hashAlgorithms =
        req.getParameter("hashAlgorithm") != null
            ? List.of(req.getParameterValues("hashAlgorithm"))
            : List.of("sha-256");
    final String contentType = req.getContentType();
    final String ignoreFields = req.getParameter("ignoreFields");
    if (!StringUtils.isBlank(ignoreFields)) {
      eventHashGenerator.excludeFieldsInPreHash(ignoreFields);
    }

    final List<String> hashParameters = getHashParameters(prehash, beautifyPreHash, hashAlgorithms);
    Multi<Map<String, String>> result =
        req.getContentType().equals("application/xml")
            ? eventHashGenerator.fromXml(
                req.getInputStream(), hashParameters.toArray(String[]::new))
            : eventHashGenerator.fromJson(
                generateJsonDocumentWrapper(req.getInputStream()),
                hashParameters.toArray(String[]::new));
    writeResult(resp, result);
  }

  // Add the outer wrapper elements for the JSON eventList when array of EPCIS events is provided.
  private InputStream generateJsonDocumentWrapper(final InputStream inputEventList)
      throws IOException {
    InputStream convertedDocument;

    try (final PipedOutputStream outTransform = new PipedOutputStream()) {
      convertedDocument = new PipedInputStream(outTransform);
      managedExecutor.execute(
          () -> {
            try (final JsonGenerator jsonGenerator = jsonFactory.createGenerator(outTransform)) {
              jsonGenerator.writeStartObject();
              jsonGenerator.writeStringField("type", "EPCISDocument");
              jsonGenerator.writeStringField("schemaVersion", "2.0");
              jsonGenerator.writeStringField("creationDate", Instant.now().toString());
              jsonGenerator.writeObjectFieldStart("epcisBody");
              jsonGenerator.writeFieldName("eventList");
              int r;
              byte[] buffer = new byte[1024];
              while ((r = inputEventList.read(buffer, 0, buffer.length)) != -1) {
                jsonGenerator.writeRawValue(new String(buffer, 0, r, StandardCharsets.UTF_8));
                jsonGenerator.flush();
              }
              jsonGenerator.writeEndObject();
              jsonGenerator.writeEndObject();
              jsonGenerator.flush();
            } catch (Exception ex) {
              try {
                outTransform.write(
                    ("Exception occurred during the creation of wrapper document for eventList : "
                            + ex.getMessage())
                        .getBytes(StandardCharsets.UTF_8));
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
              try {
                outTransform.close();
              } catch (Exception ignore) {
                // ignored
              }
            }
          });
    }
    return convertedDocument;
  }
}
