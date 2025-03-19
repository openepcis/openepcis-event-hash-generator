/*
 * Copyright 2022-2024 benelog GmbH & Co. KG
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
package io.openepcis.eventhash;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import io.openepcis.eventhash.exception.EventHashException;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.context.ManagedExecutor;

@Singleton
@RequiredArgsConstructor
public class DocumentWrapperSupport {

  private final JsonFactory jsonFactory;

  private final ManagedExecutor managedExecutor;

  public final InputStream generateJsonDocumentWrapper(final InputStream inputEventList)
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
                    + ex.getMessage(),
                ex);
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
