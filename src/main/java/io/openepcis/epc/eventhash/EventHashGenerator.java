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
package io.openepcis.epc.eventhash;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openepcis.epc.eventhash.publisher.ObjectNodePublisher;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import javax.xml.parsers.SAXParserFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventHashGenerator {

  private static final SAXParserFactory SAX_PARSER_FACTORY = SAXParserFactory.newInstance();

  static {
    try {
      SAX_PARSER_FACTORY.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  // Method called by external application to generate Event Hash for every event
  public static final Multi<String> fromJson(
      final InputStream jsonStream, final String hashAlgorithm) throws IOException {
    final ObjectNodePublisher<ObjectNode> publisher = new ObjectNodePublisher<>(jsonStream);
    final HashMap<String, String> contextHeader = new HashMap<>();

    // Loop over the list of event to read them one by one to generate event Hash.
    return Multi.createFrom()
        .publisher(publisher)
        .map(
            item -> {
              if (!item.get("type").asText().equalsIgnoreCase("EPCISDocument")) {
                final ContextNode contextNode = new ContextNode(item.fields(), contextHeader);
                final String preHashString = contextNode.toShortenedString();

                // Call the method generateHashId in HashIdGenerator to
                try {
                  return HashIdGenerator.generateHashId(
                      preHashString.replaceAll("[\n\r]", ""), hashAlgorithm);
                } catch (NoSuchAlgorithmException e) {
                  e.printStackTrace();
                }

              } else if (item.get("@context") != null) {
                final Iterator<JsonNode> contextElements = item.get("@context").elements();

                while (contextElements.hasNext()) {
                  final Iterator<Map.Entry<String, JsonNode>> contextFields =
                      contextElements.next().fields();
                  while (contextFields.hasNext()) {
                    final Map.Entry<String, JsonNode> namespace = contextFields.next();
                    contextHeader.put(namespace.getKey(), namespace.getValue().textValue());
                  }
                }
              }
              return "";
            })
        .filter(s -> !s.isEmpty());
  }

  // Method to read XML events and create pre-hash string from it.
  public static final Multi<String> fromXml(
      final InputStream xmlStream, final String hashAlgorithm) {
    final SaxHandler saxHandler = new SaxHandler();
    final Consumer<MultiEmitter<? super ContextNode>> consumer =
        contextNodeMultiEmitter -> {
          saxHandler.setEmitter(contextNodeMultiEmitter);
          try {
            SAX_PARSER_FACTORY.newSAXParser().parse(xmlStream, saxHandler);
          } catch (Exception e) {
            contextNodeMultiEmitter.fail(e);
          }
        };

    // After converting each XML event to ContextNode and storing information in rootNode, convert
    // it to pre-hash string and generate HashId out of it.
    return Multi.createFrom()
        .emitter(consumer)
        .map(
            node -> {
              try {
                return HashIdGenerator.generateHashId(
                    node.toShortenedString().replaceAll("[\n\r]", ""), hashAlgorithm);
              } catch (NoSuchAlgorithmException e) {
                log.error(e.getMessage(), e);
              }
              return "";
            })
        .filter(s -> !s.isEmpty());
  }
}
