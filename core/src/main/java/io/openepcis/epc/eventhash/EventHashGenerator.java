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
package io.openepcis.epc.eventhash;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openepcis.constants.EPCIS;
import io.openepcis.epc.eventhash.constant.ConstantEventHashInfo;
import io.openepcis.epc.eventhash.exception.EventHashException;
import io.openepcis.reactive.publisher.ObjectNodePublisher;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Consumer;
import javax.xml.parsers.SAXParserFactory;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@NoArgsConstructor
public class EventHashGenerator {
  private static final SAXParserFactory SAX_PARSER_FACTORY = SAXParserFactory.newInstance();
  private String prehashJoin = "";

  static {
    try {
      SAX_PARSER_FACTORY.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  public void prehashJoin(final String s) {
    prehashJoin = s.replace("\\n", "\n").replace("\\r", "\r");
  }

  /**
   * Method used to populate custom fields that needs to be ignored during the pre-hash generation
   *
   * @param excludeFields List of string element with field name which will be ignored during the
   *     pre-hash generation.
   */
  public void excludeFieldsInPreHash(final String excludeFields) {
    // If user has provided any values then add them to fields which needs to be omitted
    if (!StringUtils.isBlank(excludeFields)) {
      final List<String> excludeFieldsList =
          Arrays.stream(excludeFields.split(",")).map(String::trim).toList();

      // Clear the existing element if any
      ConstantEventHashInfo.getContext().clearFieldsToExclude();

      // Add the provided elements to List which will be ignored during pre-hash generation
      ConstantEventHashInfo.getContext().addFieldsToExclude(excludeFieldsList);
    }
  }

  /**
   * Generate reactive Multi stream of event hashes from JSON input
   *
   * @param jsonStream JSON input stream
   * @param contextHeader pre-defined map for @context header*
   * @param hashAlgorithm Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
   *     sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using "prehash" return pre-hash strings
   * @return hash string representation for each EPCIS event
   * @throws IOException reading of JSON file may throw exception
   */
  public Multi<String> fromJson(
      final InputStream jsonStream,
      final Map<String, String> contextHeader,
      final String hashAlgorithm)
      throws IOException {
    return internalFromJson(String.class, jsonStream, contextHeader, hashAlgorithm);
  }

  /**
   * Generate reactive Multi stream of event hashes from JSON input
   *
   * @param jsonStream JSON input stream
   * @param hashAlgorithm Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
   *     sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using "prehash" return pre-hash strings
   * @return hash string representation for each EPCIS event
   * @throws IOException reading of JSON file may throw exception
   */
  public Multi<String> fromJson(final InputStream jsonStream, final String hashAlgorithm)
      throws IOException {
    return fromJson(jsonStream, new HashMap<>(), hashAlgorithm);
  }

  /**
   * Generate reactive Multi stream of event hashes from JSON input
   *
   * @param jsonStream JSON input stream
   * @param contextHeader pre-defined map for @context header
   * @param hashAlgorithms Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
   *     sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using "prehash" return pre-hash strings
   * @return hash string map where key is hash algorithm and value is hash, representing each EPCIS
   *     event from InputStream
   * @throws IOException reading of JSON file may throw exception
   */
  public Multi<Map<String, String>> fromJson(
      final InputStream jsonStream,
      final Map<String, String> contextHeader,
      final String... hashAlgorithms)
      throws IOException {
    return internalFromJson(Map.class, jsonStream, contextHeader, hashAlgorithms);
  }

  /**
   * Generate reactive Multi stream of event hashes from JSON input
   *
   * @param jsonStream JSON input stream
   * @param hashAlgorithms Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
   *     sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using "prehash" return pre-hash strings
   * @return hash string map where key is hash algorithm and value is hash, representing each EPCIS
   *     event from InputStream
   * @throws IOException reading of JSON file may throw exception
   */
  public Multi<Map<String, String>> fromJson(
      final InputStream jsonStream, final String... hashAlgorithms) throws IOException {
    return fromJson(jsonStream, new HashMap<>(), hashAlgorithms);
  }

  private void addToContextHeader(final ObjectNode item, final Map<String, String> contextHeader) {
    if (item.get(EPCIS.CONTEXT) != null) {
      final Iterator<JsonNode> contextElements = item.get(EPCIS.CONTEXT).elements();
      contextHeader.put(EPCIS.CBV_MDA, EPCIS.CBV_MDA_URN);
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
  public Multi<String> fromPublisher(
      final Publisher<ObjectNode> publisher, final String hashAlgorithm) {
    return fromPublisher(publisher, new HashMap<>(), hashAlgorithm);
  }

  /**
   * Generate reactive Multi stream of hashes from ObjectNode publisher
   *
   * @param publisher ObjectNodePublisher
   * @param contextHeader pre-defined map for @context header
   * @param hashAlgorithm Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
   *     sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using "prehash" return pre-hash strings
   * @return hash string representation for each EPCIS event
   */
  public Multi<String> fromPublisher(
      final Publisher<ObjectNode> publisher,
      final Map<String, String> contextHeader,
      final String hashAlgorithm) {
    return internalFromPublisher(String.class, publisher, contextHeader, hashAlgorithm);
  }

  /**
   * Generate reactive Multi stream of mapped hashes from ObjectNode publisher
   *
   * @param publisher ObjectNodePublisher
   * @param contextHeader pre-defined map for @context header
   * @param hashAlgorithms Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
   *     sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using "prehash" return pre-hash strings
   * @return mapped hash string representation for each EPCIS event
   */
  public Multi<Map<String, String>> fromPublisher(
      final Publisher<ObjectNode> publisher,
      final Map<String, String> contextHeader,
      final String... hashAlgorithms) {
    return internalFromPublisher(Map.class, publisher, contextHeader, hashAlgorithms);
  }

  /**
   * Generate reactive Multi stream of mapped hashes from ObjectNode publisher
   *
   * @param publisher ObjectNodePublisher
   * @param hashAlgorithms Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
   *     sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using "prehash" return pre-hash strings
   * @return mapped hash string representation for each EPCIS event
   */
  public Multi<Map<String, String>> fromPublisher(
      final Publisher<ObjectNode> publisher, final String... hashAlgorithms) {
    return fromPublisher(publisher, new HashMap<>(), hashAlgorithms);
  }

  /**
   * Generate hashes string from single ObjectNode
   *
   * @param objectNode JSON ObjectNode
   * @param hashAlgorithm Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
   *     sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using "prehash" return pre-hash strings
   * @return hash string representation for each EPCIS event
   */
  public String fromObjectNode(final ObjectNode objectNode, final String hashAlgorithm) {
    return fromObjectNode(objectNode, new HashMap<>(), hashAlgorithm);
  }

  /**
   * Generate hashes string from single ObjectNode
   *
   * @param objectNode JSON ObjectNode
   * @param contextHeader pre-defined map for @context header*
   * @param hashAlgorithm Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
   *     sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using "prehash" return pre-hash strings
   * @return hash string representation for each EPCIS event
   */
  public String fromObjectNode(
      final ObjectNode objectNode,
      final Map<String, String> contextHeader,
      final String hashAlgorithm) {
    return internalFromObjectNode(String.class, objectNode, contextHeader, hashAlgorithm);
  }
  /**
   * Generate reactive Multi stream of mapped hashes from single ObjectNode
   *
   * @param objectNode JSON ObjectNode
   * @param hashAlgorithms Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
   *     sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. using "prehash" return pre-hash strings
   * @return mapped hash string representation for each EPCIS event
   */
  public Multi<Map<String, String>> fromObjectNode(
      final ObjectNode objectNode, final String... hashAlgorithms) {
    return internalFromObjectNode(Map.class, objectNode, new HashMap<>(), hashAlgorithms);
  }

  private <T> T internalFromObjectNode(
      final Class<? super T> cls,
      final ObjectNode objectNode,
      final Map<String, String> contextHeader,
      final String... hashAlgorithms) {
    addToContextHeader(objectNode, contextHeader);
    if (!objectNode.get(EPCIS.TYPE).asText().equalsIgnoreCase(EPCIS.EPCIS_DOCUMENT)
        && !objectNode.get(EPCIS.TYPE).asText().equalsIgnoreCase(EPCIS.EPCIS_QUERY_DOCUMENT)) {
      final ContextNode contextNode = new ContextNode(objectNode.fields(), contextHeader);
      final String preHashString = contextNode.toShortenedString();

      // Call the method generateHashId in HashIdGenerator to
      return generate(cls, preHashString, hashAlgorithms);
    }

    if (cls.isAssignableFrom(String.class)) {
      return (T) "";
    } else {
      return (T) Collections.<String, String>emptyMap();
    }
  }

  private <T> Multi<T> internalFromPublisher(
      final Class<? super T> cls,
      final Publisher<ObjectNode> publisher,
      final Map<String, String> contextHeader,
      final String... hashAlgorithms) {
    return Multi.createFrom()
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

  private <T> Multi<T> internalFromJson(
      final Class<? super T> cls,
      final InputStream jsonStream,
      final Map<String, String> contextHeader,
      final String... hashAlgorithms)
      throws IOException {
    final Publisher publisher =
        new ObjectNodePublisher(jsonStream);
    return internalFromPublisher(cls, publisher, contextHeader, hashAlgorithms);
  }

  protected <T> T generate(
      final Class<? super T> cls, final String s, final String[] hashAlgorithms)
      throws RuntimeException {
    try {
      if (cls.isAssignableFrom(String.class)) {
        if (hashAlgorithms.length != 1) {
          throw new EventHashException("only one single algorithm allowed for type String");
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
    } catch (Exception e) {
      throw new EventHashException(
          "Exception occurred during event hash generation : " + e.getMessage(), e);
    }
  }

  private <T> Multi<T> internalFromXml(
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
  public Multi<String> fromXml(final InputStream xmlStream, final String hashAlgorithm) {
    return internalFromXml(String.class, xmlStream, hashAlgorithm);
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
  public Multi<Map<String, String>> fromXml(
      final InputStream xmlStream, final String... hashAlgorithms) {
    return internalFromXml(Map.class, xmlStream, hashAlgorithms);
  }
}
