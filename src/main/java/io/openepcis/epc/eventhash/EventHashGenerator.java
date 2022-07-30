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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import javax.xml.parsers.SAXParserFactory;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;

@Slf4j
public class EventHashGenerator {

  private static final SAXParserFactory SAX_PARSER_FACTORY = SAXParserFactory.newInstance();

  private static String prehashJoin = "";

  static {
    try {
      SAX_PARSER_FACTORY.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  private EventHashGenerator() {}

  public static void prehashJoin(final String s) {
    EventHashGenerator.prehashJoin = s.replace("\\n", "\n").replace("\\r", "\r");
  }

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
    return EventHashGenerator.internalFromJson(String.class, jsonStream, hashAlgorithm);
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
    return EventHashGenerator.internalFromJson(Map.class, jsonStream, hashAlgorithms);
  }

  private static void addToContextHeader(
      final ObjectNode item, final Map<String, String> contextHeader) {
    if (item.get("@context") != null) {
      final Iterator<JsonNode> contextElements = item.get("@context").elements();

      while (contextElements.hasNext()) {
        final Iterator<Map.Entry<String, JsonNode>> contextFields = contextElements.next().fields();
        while (contextFields.hasNext()) {
          final Map.Entry<String, JsonNode> namespace = contextFields.next();
          contextHeader.put(namespace.getKey(), namespace.getValue().textValue());
        }
      }
    }
  }

  /**
   * Generate reactive Multi stream of hashes from ObjectNode publisher
   *
   * @param publisher ObjectNodePublisher
   * @param hashAlgorithm Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
   *     sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using "prehash" return pre-hash strings
   * @return hash string representation for each EPCIS event
   */
  public static Multi<String> fromPublisher(
      final Publisher<ObjectNode> publisher, final String hashAlgorithm) {
    return internalFromPublisher(String.class, publisher, new String[] {hashAlgorithm});
  }

  /**
   * Generate reactive Multi stream of mapped hashes from ObjectNode publisher
   *
   * @param publisher ObjectNodePublisher
   * @param hashAlgorithms Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
   *     sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using "prehash" return pre-hash strings
   * @return mapped hash string representation for each EPCIS event
   */
  public static Multi<Map<String, String>> fromPublisher(
      final Publisher<ObjectNode> publisher, final String... hashAlgorithms) {
    return internalFromPublisher(Map.class, publisher, hashAlgorithms);
  }

  /**
   * Generate hashes string from single ObjectNode
   *
   * @param objectNode JSON ObjectNode
   * @param hashAlgorithm Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
   *     sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using "prehash" return pre-hash strings
   * @return hash string representation for each EPCIS event
   */
  public static String fromObjectNode(final ObjectNode objectNode, final String hashAlgorithm) {
    return internalFromObjectNode(String.class, objectNode, new HashMap<>(), hashAlgorithm);
  }

  /**
   * Generate reactive Multi stream of mapped hashes from single ObjectNode
   *
   * @param objectNode JSON ObjectNode
   * @param hashAlgorithms Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
   *     sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using "prehash" return pre-hash strings
   * @return mapped hash string representation for each EPCIS event
   */
  public static Multi<Map<String, String>> fromObjectNode(
      final ObjectNode objectNode, final String... hashAlgorithms) {
    return internalFromObjectNode(Map.class, objectNode, new HashMap<>(), hashAlgorithms);
  }

  private static <T> T internalFromObjectNode(
      final Class<? super T> cls,
      final ObjectNode objectNode,
      final Map<String, String> contextHeader,
      final String... hashAlgorithms) {
    addToContextHeader(objectNode, contextHeader);
    if (!objectNode.get("type").asText().equalsIgnoreCase("EPCISDocument")) {
      final ContextNode contextNode = new ContextNode(objectNode.fields(), contextHeader);
      final String preHashString = contextNode.toShortenedString();

      // Call the method generateHashId in HashIdGenerator to
      return (T) generate(cls, preHashString, hashAlgorithms);
    }
    if (cls.isAssignableFrom(String.class)) {
      return (T) "";
    } else {
      return (T) Collections.<String, String>emptyMap();
    }
  }

  private static <T> Multi<T> internalFromPublisher(
      final Class<? super T> cls,
      final Publisher<ObjectNode> publisher,
      final String... hashAlgorithms) {
    // map event ObjectNodes to event Hash.
    final Map<String, String> contextHeader = new HashMap<>();
    return (Multi<T>)
        Multi.createFrom()
            .publisher(publisher)
            .map(item -> (T) internalFromObjectNode(cls, item, contextHeader, hashAlgorithms))
            .filter(
                m -> {
                  if (cls.isAssignableFrom(String.class)) {
                    return !((String) m).isEmpty();
                  }
                  return !((Map) m).isEmpty();
                });
  }

  private static <T> Multi<T> internalFromJson(
      final Class<? super T> cls, final InputStream jsonStream, final String... hashAlgorithms)
      throws IOException {
    final ObjectNodePublisher<ObjectNode> publisher = new ObjectNodePublisher<>(jsonStream);
    return internalFromPublisher(cls, publisher, hashAlgorithms);
  }

  protected static <T> T generate(
      final Class<? super T> cls, final String s, final String[] hashAlgorithms)
      throws RuntimeException {
    try {
      if (cls.isAssignableFrom(String.class)) {
        if (hashAlgorithms.length != 1) {
          throw new RuntimeException("only one single algorithm allowed for type String");
        }
        return (T) HashIdGenerator.generateHashId(s.replaceAll("[\n\r]", ""), hashAlgorithms[0]);
      }
      final Map<String, String> map = new HashMap<>();
      for (final String hashAlgorithm : hashAlgorithms) {
        if (hashAlgorithm.equalsIgnoreCase("prehash")) {
          map.put(hashAlgorithm, s.replaceAll("[\n\r]+", prehashJoin));
        } else {
          map.put(
              hashAlgorithm,
              HashIdGenerator.generateHashId(s.replaceAll("[\n\r]", ""), hashAlgorithm));
        }
      }
      return (T) map;
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private static <T> Multi<T> internalFromXml(
      final Class<? super T> cls, final InputStream xmlStream, final String... hashAlgorithms) {
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
    return (Multi<T>)
        Multi.createFrom()
            .emitter(consumer)
            .map(node -> generate(cls, node.toShortenedString(), hashAlgorithms))
            .filter(
                m -> {
                  if (cls.isAssignableFrom(String.class)) {
                    return !((String) m).isEmpty();
                  }
                  return !((Map<String, String>) m).isEmpty();
                });
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
    return EventHashGenerator.internalFromXml(String.class, xmlStream, hashAlgorithm);
  }

  /**
   * Generate reactive Multi stream of event hashes from XML input
   *
   * @param xmlStream XML input stream
   * @param hashAlgorithms Type of Hash Algorithms to run: sha-1, sha-224, sha-256, sha-384,
   *     sha-512, sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using pre return pre-hash
   *     strings
   * @return mapped hash string map where key is hash algorithm and value is hash, representing each
   *     EPCIS event from InputStream
   */
  public static Multi<Map<String, String>> fromXml(
      final InputStream xmlStream, final String... hashAlgorithms) {
    return EventHashGenerator.internalFromXml(Map.class, xmlStream, hashAlgorithms);
  }
}
