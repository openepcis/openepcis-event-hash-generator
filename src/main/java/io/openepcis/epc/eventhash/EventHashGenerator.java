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
import java.util.*;
import java.util.function.Consumer;
import javax.xml.parsers.SAXParserFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventHashGenerator {

  private static final SAXParserFactory SAX_PARSER_FACTORY = SAXParserFactory.newInstance();

  private static String PREHASH_JOIN = "";

  public static void prehashJoin(final String s) {
    EventHashGenerator.PREHASH_JOIN = s.replace("\\n", "\n").replace("\\r", "\r");
  }

  static {
    try {
      SAX_PARSER_FACTORY.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  private EventHashGenerator() {}

  /**
   * Generate reactive Multi stream of event hashes from JSON input
   *
   * @param jsonStream JSON input stream
   * @param hashAlgorithm Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
   *     sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using "prehash" return pre-hash strings
   * @return hash string representation for each EPCIS event
   * @throws IOException
   */
  public static Multi<String> fromJson(final InputStream jsonStream, final String hashAlgorithm)
      throws IOException {
    return fromJson(jsonStream, new String[] {hashAlgorithm}).map(item -> item.get(hashAlgorithm));
  }

  /**
   * Generate reactive Multi stream of event hashes from JSON input
   *
   * @param jsonStream JSON input stream
   * @param hashAlgorithms Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
   *     sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using "prehash" return pre-hash strings
   * @return hash string map where key is hash algorithm and value is hash, representing each EPCIS
   *     event from InputStream
   * @throws IOException
   */
  public static Multi<Map<String, String>> fromJson(
      final InputStream jsonStream, final String... hashAlgorithms) throws IOException {
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
                return generate(preHashString, hashAlgorithms);

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
              return Collections.<String, String>emptyMap();
            })
        .filter(m -> !m.isEmpty());
  }

  protected static Map<String, String> generate(final String s, final String[] hashAlgorithms)
      throws RuntimeException {
    try {
      final Map<String, String> map = new HashMap<>();
      for (final String hashAlgorithm : hashAlgorithms) {
        if (hashAlgorithm.toLowerCase().equals("prehash")) {
          map.put(hashAlgorithm, s.replaceAll("[\n\r]+", PREHASH_JOIN));
        } else {
          map.put(
              hashAlgorithm,
              HashIdGenerator.generateHashId(s.replaceAll("[\n\r]", ""), hashAlgorithm));
        }
      }
      return map;
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
  /**
   * Generate reactive Multi stream of event hashes from XML input
   *
   * @param xmlStream XML input stream
   * @param hashAlgorithm Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
   *     sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using pre return pre-hash strings
   * @return hash string representation for each EPCIS event
   */
  public static Multi<String> fromXml(final InputStream xmlStream, final String hashAlgorithm) {
    return fromXml(xmlStream, new String[] {hashAlgorithm}).map(m -> m.get(hashAlgorithm));
  }

  /**
   * Generate reactive Multi stream of event hashes from XML input
   *
   * @param xmlStream XML input stream
   * @param hashAlgorithms Type of Hash Algorithms to run: sha-1, sha-224, sha-256, sha-384,
   *     sha-512, sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using pre return pre-hash
   *     strings
   * @return hash string map where key is hash algorithm and value is hash, representing each EPCIS
   *     event from InputStream
   */
  public static Multi<Map<String, String>> fromXml(
      final InputStream xmlStream, final String... hashAlgorithms) {
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
        .map(node -> generate(node.toShortenedString(), hashAlgorithms))
        .filter(m -> !m.isEmpty());
  }
}
