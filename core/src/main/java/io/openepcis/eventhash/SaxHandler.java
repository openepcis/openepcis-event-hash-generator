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

import io.openepcis.constants.EPCIS;
import io.openepcis.eventhash.constant.ConstantEventHashInfo;
import io.smallrye.mutiny.subscription.MultiEmitter;
import lombok.Setter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class SaxHandler extends DefaultHandler {
    // Variables needed to store the required information during the parsing of the XML document for every EPCIS event.
    private final Deque<String> path = new ArrayDeque<>();                          // current XML element stack
    private final StringBuilder currentValue = new StringBuilder();                 // accumulator for #characters callbacks
    private ContextNode currentNode = null;                                         // walking pointer into the tree
    private ContextNode rootNode = null;                                            // per-event root; reset to null after emit
    private Map<String, String> currentAttributes;                                  // attributes of the open element
    private final HashMap<String, String> contextHeader = new HashMap<>();          // xmlns:* bindings seen so far

    @Setter
    private MultiEmitter<? super ContextNode> emitter;

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
        // Track the open-element stack regardless of whether we record this path
        path.push(qName);

        // Skip excluded paths (errorDeclaration, recordTime, etc.) entirely — no state update
        if (isExcludePath()) return;

        // Reset per-element attribute store, then split incoming attributes into xmlns vs. local
        currentAttributes = new HashMap<>();
        parseAttributes(attributes);

        // Walk-down: enter a new event root, an unnamed why-dimension child, or a named child
        rootNodePopulate(qName);
    }


    @Override
    public void characters(char[] ch, int start, int length) {
        // Only accumulate text inside paths we care about and ignore the other fields
        if (!isExcludePath()) {
            currentValue.append(ch, start, length);
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) {
        // Excluded path: pop the stack and exit — no tree mutation, no state reset of currentValue/attributes
        if (isExcludePath()) {
            path.pop();
            return;
        }

        // Apply the close-tag semantics for whichever dimension this element belongs to
        if (rootNode != null && !ConstantEventHashInfo.EXCLUDE_XML_FIELDS.contains(qName)) {
            xmlParser(qName);
        }

        // End-of-event detection — emit the accumulated tree and start a fresh root for the next event
        if (isEpcisEventType(qName)) {
            emitAndReset();
        }

        // Per-element cleanup so the next sibling/child starts with a clean slate
        resetPerElementState();
        path.pop();
    }

    @Override
    public void endDocument() throws SAXException {
        super.endDocument();
        emitter.complete();
    }

    // ---------------------------------------------------------------------------
    // Sub-dispatchers
    // ---------------------------------------------------------------------------

    // Decide how a new open-element should mutate the tree pointer.
    private void rootNodePopulate(final String qName) {
        final String xmlPath = getXMLPath();

        if (rootNode == null && isEpcisEventType(qName)) {
            enterEpcisEvent(qName);
        } else if (currentNode != null && isInWhyDimension(xmlPath)) {
            enterUnnamedChild();
        } else if (currentNode != null && !isInWhatDimension(xmlPath)) {
            enterNamedChild(qName);
        }
    }

    // Decide how a closing element contributes the leaf value to the tree.
    private void xmlParser(final String qName) {
        final String xmlPath = getXMLPath();
        final String value = currentValueOrNull();

        if (isInWhatDimension(xmlPath)) {
            recordWhatLeaf(value);
        } else if (isInWhyDimension(xmlPath) || isInHowDimension(xmlPath)) {
            recordWhyOrHowLeaf(qName, value);
        } else if (currentNode != null) {
            recordPlainLeaf(value);
        }
    }


    // ---------------------------------------------------------------------------
    // Predicates — path-level decisions
    // ---------------------------------------------------------------------------

    // True when the current XML path matches one of the always-excluded fields (errorDeclaration, recordTime, etc.).
    private boolean isExcludePath() {
        final String xmlPath = getXMLPath();
        return ConstantEventHashInfo.DEFAULT_FIELDS_TO_EXCLUDE_IN_PREHASH.stream().anyMatch(xmlPath::contains);
    }

    // True if the path is inside one of the WHAT-dimension lists (epcList, childEPCs, etc.).
    private boolean isInWhatDimension(final String xmlPath) {
        return ConstantEventHashInfo.WHAT_DIMENSION_XML_PATH.stream().anyMatch(xmlPath::startsWith);
    }

    // True if the path is inside one of the WHY-dimension lists (bizTransactionList, sourceList, destinationList).
    private boolean isInWhyDimension(final String xmlPath) {
        return ConstantEventHashInfo.WHY_DIMENSION_XML_PATH.stream().anyMatch(xmlPath::startsWith);
    }

    // True if the path is inside the HOW-dimension lists (sensorElement nested under sensorElementList, sensorReport).
    private boolean isInHowDimension(final String xmlPath) {
        return ConstantEventHashInfo.HOW_DIMENSION_XML_PATH.stream().anyMatch(xmlPath::startsWith);
    }

    // True when qName is one of the EPCIS event element names (ObjectEvent, AggregationEvent, …).
    private boolean isEpcisEventType(final String qName) {
        return ConstantEventHashInfo.EPCIS_EVENT_TYPES.contains(qName);
    }

    // ---------------------------------------------------------------------------
    // Actions — tree mutations and per-element state changes
    // ---------------------------------------------------------------------------

    // Split incoming SAX attributes into xmlns:* namespace bindings vs. element-local attributes.
    private void parseAttributes(final Attributes attributes) {
        for (int i = 0; i < attributes.getLength(); i++) {
            final String qName = attributes.getQName(i);
            final String value = attributes.getValue(i).trim();

            if (qName.startsWith("xmlns:")) {
                contextHeader.put(qName.substring(qName.indexOf(":") + 1), value);
            } else {
                currentAttributes.put(qName, value);
            }
        }
    }

    // Begin a new EPCIS event: allocate a fresh root tree and add the event-type child.
    private void enterEpcisEvent(final String qName) {
        rootNode = new ContextNode(contextHeader);
        currentNode = rootNode;
        rootNode.children.add(new ContextNode(rootNode, EPCIS.TYPE, qName));
    }

    // Walk into a new unnamed subtree (used for why-dimension list items where the wrapper tag is unnamed).
    private void enterUnnamedChild() {
        final ContextNode n = new ContextNode(currentNode, null, (String) null);
        currentNode.children.add(n);
        currentNode = n;
    }

    // Walk into a named subtree under the current node.
    private void enterNamedChild(final String qName) {
        final ContextNode child = new ContextNode(currentNode, qName, (String) null);
        currentNode.children.add(child);
        currentNode = child;
    }

    // WHAT-dimension leaf: simple child with the current tag name and accumulated value.
    private void recordWhatLeaf(final String value) {
        currentNode.children.add(new ContextNode(currentNode, path.peek(), value));
    }

    // WHY/HOW-dimension leaf: emit attribute children, then the value child, then walk back up.
    private void recordWhyOrHowLeaf(final String qName, final String value) {
        if (MapUtils.isNotEmpty(currentAttributes)) {
            currentAttributes.forEach((attrKey, attrValue) -> currentNode.children.add(new ContextNode(currentNode, attrKey, attrValue)));
            currentNode.children.add(new ContextNode(currentNode, qName, value));
        } else if (value != null) {
            currentNode.children.add(new ContextNode(currentNode, qName, value));
        }
        currentNode = currentNode.parent;
    }

    // Default leaf: set value on the current node, add filtered attributes, walk back up.
    private void recordPlainLeaf(final String value) {
        currentNode.setValue(value);
        if (MapUtils.isNotEmpty(currentAttributes)) {
            currentAttributes.entrySet().stream()
                    .filter(attr -> !attr.getKey().startsWith("xsi:") && !attr.getValue().startsWith("xsd:"))
                    .forEach(attr -> currentNode.children.add(new ContextNode(currentNode, attr.getKey(), attr.getValue())));
        }

        // Walk back up to the parent so the next sibling element starts at the correct tree level.
        currentNode = currentNode.parent;
    }

    // Emit the just-built event tree to the downstream Multi and discard the reference so the next event starts fresh.
    private void emitAndReset() {
        emitter.emit(rootNode);
        rootNode = null;
    }

    // Reset per-element scratch state (text-content buffer + attributes) so the next sibling starts clean.
    private void resetPerElementState() {
        currentValue.setLength(0);
        currentAttributes = new HashMap<>();
    }

    // Return the trimmed accumulated #characters text, or null if blank.
    private String currentValueOrNull() {
        return !StringUtils.isBlank(currentValue) ? currentValue.toString().trim() : null;
    }

    // Build the canonical "/" -separated XML path from the open-element stack.
    private String getXMLPath() {
        return String.join(EPCIS.PATH_DELIMITER, this.path);
    }

}
