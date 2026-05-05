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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openepcis.constants.CBVVersion;
import io.openepcis.constants.EPCIS;
import io.openepcis.eventhash.exception.EventHashException;
import io.openepcis.reactive.publisher.ObjectNodePublisher;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.MultiEmitter;
import lombok.extern.slf4j.Slf4j;

import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Generates canonical EPCIS 2.0 event hash IDs from XML or JSON/JSON-LD via reactive {@link Multi}.
 * Thread-safe; backpressure-bounded (override with {@code -Dopenepcis.eventhash.maxPendingEvents=N}); {@code contextHeader} maps are defensively copied.
 * Pass {@code "prehash"} as the algorithm name to get the pre-hash string itself.
 */

@Slf4j
public class EventHashGenerator {
    private static final SAXParserFactory SAX_PARSER_FACTORY = SAXParserFactory.newInstance();
    private String prehashJoin = "";
    private final CBVVersion cbvVersion;

    // Pre-compiled once and reused; avoids per-event Pattern.compile on the hot path.
    private static final Pattern NEWLINE = Pattern.compile("[\n\r]");
    private static final Pattern NEWLINE_RUN = Pattern.compile("[\n\r]+");

    // Cap pending events so a slow consumer fails fast with BackPressureFailure instead of OOMing. Override per-JVM with -Dopenepcis.eventhash.maxPendingEvents=N
    private static final int MAX_PENDING_EVENTS = Integer.getInteger("openepcis.eventhash.maxPendingEvents", 4096);


    static {
        try {
            SAX_PARSER_FACTORY.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Default constructor which generates the pre-hash string based on CBV 2.0
     */
    public EventHashGenerator() {
        this.cbvVersion = CBVVersion.VERSION_2_0_0;
    }

    /**
     * Constructor which generates the pre-hash string based on provided CBV version
     *
     * @param cbvVersion required CBV version that needs to be used for pre-hash string generation.
     */
    public EventHashGenerator(final CBVVersion cbvVersion) {
        this.cbvVersion = cbvVersion != null ? cbvVersion : CBVVersion.VERSION_2_0_0;
    }

    /**
     * Sets the display separator between fields in {@code "prehash"} output; does not affect digest bytes.
     */
    public void prehashJoin(final String s) {
        prehashJoin = s.replace("\\n", "\n").replace("\\r", "\r");
    }

    // ---------------------------------------------------------------------------
    // Public API — XML
    // ---------------------------------------------------------------------------

    /**
     * Generate reactive Multi stream of event hashes from XML input.
     *
     * @param xmlStream     XML input stream
     * @param hashAlgorithm Type of Hash Algorithm to run: sha-1, sha-224, sha-256, sha-384, sha-512,
     *                      sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. Using "prehash" returns the pre-hash string.
     * @return hash string for each EPCIS event
     */
    public Multi<String> fromXml(final InputStream xmlStream, final String hashAlgorithm) {
        return parseXmlEvents(xmlStream)
                .map(node -> generateString(node.toShortenedString(this.cbvVersion), hashAlgorithm))
                .filter(s -> !s.isEmpty());
    }

    /**
     * Generate reactive Multi stream of mapped hashes from XML input.
     *
     * @param xmlStream      XML input stream
     * @param hashAlgorithms One or more hash algorithms (e.g. "prehash", "sha-256").
     * @return mapped hash for each EPCIS event keyed by algorithm name
     */
    public Multi<Map<String, String>> fromXml(final InputStream xmlStream, final String... hashAlgorithms) {
        return parseXmlEvents(xmlStream)
                .map(node -> generateMap(node.toShortenedString(this.cbvVersion), hashAlgorithms))
                .filter(m -> !m.isEmpty());
    }

    // ---------------------------------------------------------------------------
    // Public API — JSON
    // ---------------------------------------------------------------------------

    /**
     * Generate reactive Multi stream of event hashes from JSON input.
     *
     * @param jsonStream    JSON input stream
     * @param contextHeader pre-defined map for @context header (defensively copied; not mutated)
     * @param hashAlgorithm hash algorithm name
     * @return hash string for each EPCIS event
     */
    public Multi<String> fromJson(final InputStream jsonStream, final Map<String, String> contextHeader, final String hashAlgorithm) throws IOException {
        final Map<String, String> owned = new HashMap<>(contextHeader);
        final Publisher<ObjectNode> publisher = ObjectNodePublisher.fromInputStream(jsonStream);
        return Multi.createFrom()
                .publisher(publisher)
                .map(item -> singleEventAsString(item, owned, hashAlgorithm))
                .filter(s -> !s.isEmpty())
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    /**
     * Generate reactive Multi stream of event hashes from JSON input.
     *
     * @param jsonStream    JSON input stream
     * @param hashAlgorithm hash algorithm name
     */
    public Multi<String> fromJson(final InputStream jsonStream, final String hashAlgorithm) throws IOException {
        return fromJson(jsonStream, new HashMap<>(), hashAlgorithm);
    }

    /**
     * Generate reactive Multi stream of mapped hashes from JSON input.
     *
     * @param jsonStream     JSON input stream
     * @param contextHeader  pre-defined map for @context header (defensively copied; not mutated)
     * @param hashAlgorithms one or more hash algorithm names
     */
    public Multi<Map<String, String>> fromJson(final InputStream jsonStream, final Map<String, String> contextHeader, final String... hashAlgorithms) throws IOException {
        final Map<String, String> owned = new HashMap<>(contextHeader);
        final Publisher<ObjectNode> publisher = ObjectNodePublisher.fromInputStream(jsonStream);
        return Multi.createFrom()
                .publisher(publisher)
                .map(item -> singleEventAsMap(item, owned, hashAlgorithms))
                .filter(m -> !m.isEmpty())
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    /**
     * Generate reactive Multi stream of mapped hashes from JSON input.
     *
     * @param jsonStream     JSON input stream
     * @param hashAlgorithms one or more hash algorithm names
     */
    public Multi<Map<String, String>> fromJson(final InputStream jsonStream, final String... hashAlgorithms) throws IOException {
        return fromJson(jsonStream, new HashMap<>(), hashAlgorithms);
    }

    // ---------------------------------------------------------------------------
    // Public API — ObjectNode publisher (caller-supplied)
    // ---------------------------------------------------------------------------

    /**
     * Generate hashes from a caller-supplied {@code ObjectNode} {@link Publisher}.
     */

    public Multi<String> fromPublisher(final Publisher<ObjectNode> publisher, final String hashAlgorithm) {
        return fromPublisher(publisher, new HashMap<>(), hashAlgorithm);
    }

    /**
     * Generate hashes from a {@link Publisher} with a pre-populated {@code @context} header (defensively copied).
     */
    public Multi<String> fromPublisher(final Publisher<ObjectNode> publisher, final Map<String, String> contextHeader, final String hashAlgorithm) {
        final Map<String, String> owned = new HashMap<>(contextHeader);
        return Multi.createFrom()
                .publisher(publisher)
                .map(item -> singleEventAsString(item, owned, hashAlgorithm))
                .filter(s -> !s.isEmpty());
    }

    /**
     * Generate per-algorithm hash maps from a {@link Publisher} with a pre-populated {@code @context} header (defensively copied).
     */
    public Multi<Map<String, String>> fromPublisher(final Publisher<ObjectNode> publisher, final Map<String, String> contextHeader, final String... hashAlgorithms) {
        final Map<String, String> owned = new HashMap<>(contextHeader);
        return Multi.createFrom()
                .publisher(publisher)
                .map(item -> singleEventAsMap(item, owned, hashAlgorithms))
                .filter(m -> !m.isEmpty());
    }

    /**
     * Generate per-algorithm hash maps from a caller-supplied {@link Publisher}.
     */
    public Multi<Map<String, String>> fromPublisher(final Publisher<ObjectNode> publisher, final String... hashAlgorithms) {
        return fromPublisher(publisher, new HashMap<>(), hashAlgorithms);
    }

    // ---------------------------------------------------------------------------
    // Public API — single-shot ObjectNode
    // ---------------------------------------------------------------------------

    /**
     * Synchronously hash a single ObjectNode event with one algorithm; returns empty string for document wrappers.
     */
    public String fromObjectNode(final ObjectNode objectNode, final String hashAlgorithm) {
        return fromObjectNode(objectNode, new HashMap<>(), hashAlgorithm);
    }

    /**
     * Synchronously hash a single ObjectNode event with a caller-supplied {@code @context} header (defensively copied).
     */
    public String fromObjectNode(final ObjectNode objectNode, final Map<String, String> contextHeader, final String hashAlgorithm) {
        return singleEventAsString(objectNode, new HashMap<>(contextHeader), hashAlgorithm);
    }

    /**
     * Generate reactive Multi (of one item) holding the mapped hashes for a single ObjectNode.
     * Result map is keyed by algorithm name; "prehash" returns the pre-hash string.
     */
    public Multi<Map<String, String>> fromObjectNode(final ObjectNode objectNode, final String... hashAlgorithms) {
        return Multi.createFrom()
                .item(() -> singleEventAsMap(objectNode, new HashMap<>(), hashAlgorithms))
                .filter(m -> !m.isEmpty());
    }

    // ---------------------------------------------------------------------------
    // Private helpers — typed, no Class<?> tokens, no unchecked casts
    // ---------------------------------------------------------------------------

    /**
     * True if this ObjectNode is an EPCIS document wrapper rather than an event.
     */
    private static boolean isDocumentWrapper(final ObjectNode item) {
        final String type = item.get(EPCIS.TYPE).asText();
        return type.equalsIgnoreCase(EPCIS.EPCIS_DOCUMENT) || type.equalsIgnoreCase(EPCIS.EPCIS_QUERY_DOCUMENT);
    }

    /**
     * Merge any @context bindings from a single event into the (already-owned) context header.
     */
    private void addToContextHeader(final ObjectNode item, final Map<String, String> contextHeader) {
        if (item.get(EPCIS.CONTEXT) != null) {
            final Iterator<JsonNode> contextElements = item.get(EPCIS.CONTEXT).elements();
            contextHeader.put(EPCIS.CBV_MDA, EPCIS.CBV_MDA_URN);
            contextHeader.put(EPCIS.GS1, EPCIS.GS1_VOC_DOMAIN);
            while (contextElements.hasNext()) {
                for (Map.Entry<String, JsonNode> namespace : contextElements.next().properties()) {
                    contextHeader.put(namespace.getKey(), namespace.getValue().textValue());
                }
            }
        }
    }

    /**
     * Stream EPCIS events parsed from an XML InputStream as ContextNode trees.
     * Shared SAX setup for both String and Map output variants of fromXml(...).
     */
    private Multi<ContextNode> parseXmlEvents(final InputStream xmlStream) {
        final SaxHandler saxHandler = new SaxHandler();
        final Consumer<MultiEmitter<? super ContextNode>> consumer =
                emitter -> {
                    saxHandler.setEmitter(emitter);
                    try {
                        SAX_PARSER_FACTORY.newSAXParser().parse(xmlStream, saxHandler);
                    } catch (Exception e) {
                        emitter.fail(e);
                    }
                };
        return Multi.createFrom()
                .emitter(consumer)
                // Bounded queue: BackPressureFailure if a slow consumer falls behind, instead of silent OOM.
                .onOverflow().buffer(MAX_PENDING_EVENTS)
                // SAX parse is blocking; run subscription on Mutiny's worker pool so the caller's
                // thread (e.g. Vert.x I/O thread) is never frozen by parsing.
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    /**
     * Build the pre-hash string for a single ObjectNode event, then hash with one algorithm.
     */
    private String singleEventAsString(final ObjectNode item, final Map<String, String> contextHeader, final String hashAlgorithm) {
        addToContextHeader(item, contextHeader);
        if (isDocumentWrapper(item)) {
            return "";
        }
        final ContextNode tree = new ContextNode(item.properties().iterator(), contextHeader);
        return generateString(tree.toShortenedString(this.cbvVersion), hashAlgorithm);
    }

    /**
     * Build the pre-hash string for a single ObjectNode event, then hash with N algorithms.
     */
    private Map<String, String> singleEventAsMap(final ObjectNode item, final Map<String, String> contextHeader, final String... hashAlgorithms) {
        addToContextHeader(item, contextHeader);
        if (isDocumentWrapper(item)) {
            return Collections.emptyMap();
        }
        final ContextNode tree = new ContextNode(item.properties().iterator(), contextHeader);
        return generateMap(tree.toShortenedString(this.cbvVersion), hashAlgorithms);
    }

    /**
     * Strip display newlines and produce a single hash string.
     */
    private String generateString(final String s, final String hashAlgorithm) {
        try {
            final String stripped = NEWLINE.matcher(s).replaceAll("");
            return HashIdGenerator.generateHashId(stripped, hashAlgorithm, this.cbvVersion);
        } catch (Exception e) {
            throw new EventHashException("Exception occurred during event hash generation : " + e.getMessage(), e);
        }
    }

    /**
     * Strip display newlines once, then produce a map of algorithm-name to hash (or prehash output).
     */
    private Map<String, String> generateMap(final String s, final String... hashAlgorithms) {
        try {
            final String stripped = NEWLINE.matcher(s).replaceAll("");
            final Map<String, String> result = new HashMap<>();
            for (final String hashAlgorithm : hashAlgorithms) {
                if (hashAlgorithm.equalsIgnoreCase("prehash")) {
                    result.put(hashAlgorithm, NEWLINE_RUN.matcher(s).replaceAll(prehashJoin));
                } else {
                    result.put(hashAlgorithm, HashIdGenerator.generateHashId(stripped, hashAlgorithm, this.cbvVersion));
                }
            }
            return result;
        } catch (Exception e) {
            throw new EventHashException("Exception occurred during event hash generation : " + e.getMessage(), e);
        }
    }
}
