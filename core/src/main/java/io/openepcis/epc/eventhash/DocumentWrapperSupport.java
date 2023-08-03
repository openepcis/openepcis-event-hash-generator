package io.openepcis.epc.eventhash;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import io.openepcis.epc.eventhash.exception.EventHashException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.context.ManagedExecutor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Singleton
@RequiredArgsConstructor
public class DocumentWrapperSupport {


    private  final JsonFactory jsonFactory;

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
                                        + ex.getMessage(), ex);
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
