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

import io.smallrye.mutiny.subscription.MultiEmitter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SaxHandler extends DefaultHandler {

  // Variables needed to store the required information during the parsing of the XML document for
  // every EPCIS event.
  private final Deque<String> path = new ArrayDeque<>();
  private final StringBuilder currentValue = new StringBuilder();
  private ContextNode currentNode = null;
  private ContextNode rootNode = null;
  private Map<String, String> currentAttributes;
  private final HashMap<String, String> contextHeader = new HashMap<>();
  @Setter private MultiEmitter<? super ContextNode> emitter;

  @Override
  public void startElement(
      final String uri, final String localName, final String qName, final Attributes attributes) {
    // Put every XML tag within the stack at the beginning of the XML tag.
    path.push(qName);

    // Ignore the non-required elements such as errorDeclaration, recordTime, etc.
    if (ConstantEventHashInfo.IGNORE_FIELDS.stream().anyMatch(getXMLPath()::contains)) {
      return;
    }

    // Reset attributes for every element
    currentAttributes = new HashMap<>();

    // Get the path from Deque as / separated values.
    final String p = getXMLPath();

    // If the XML tag contains the Namespaces or attributes then add to respective Namespaces Map or
    // Attributes Map.
    if (attributes.getLength() > 0) {
      // Loop over every attribute and add them to respective Map.
      for (int att = 0; att < attributes.getLength(); att++) {
        // If the attributes contain the : then consider them as namespaces otherwise as the fields
        // such as type, source, etc. of the EPCIS event
        if (attributes.getQName(att).contains(":")
            && attributes.getQName(att).startsWith("xmlns:")) {
          contextHeader.put(
              attributes.getQName(att).substring(attributes.getQName(att).indexOf(":") + 1),
              attributes.getValue(att));
        } else {
          currentAttributes.put(attributes.getQName(att), attributes.getValue(att).trim());
        }
      }
    }

    // If EPCIS event type is found then create a new rootNode to store and create fresh pre-hash
    // string.
    if (rootNode == null && ConstantEventHashInfo.EPCIS_EVENT_TYPES.contains(qName)) {
      rootNode = new ContextNode(contextHeader);
      currentNode = rootNode;
      rootNode.children.add(new ContextNode(rootNode, "type", qName));
    } else if (currentNode != null
        && ConstantEventHashInfo.WHY_DIMENSION_XML_PATH.stream().anyMatch(p::startsWith)) {
      ContextNode n = new ContextNode(currentNode, null, (String) null);
      currentNode.children.add(n);
      currentNode = n;
    } else if (currentNode != null
        && ConstantEventHashInfo.WHAT_DIMENSION_XML_PATH.stream().noneMatch(p::startsWith)) {
      ContextNode n = new ContextNode(currentNode, qName, (String) null);
      currentNode.children.add(n);
      currentNode = n;
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) {
    // Ignore the non-required elements such as errorDeclaration, recordTime, etc.
    if (ConstantEventHashInfo.IGNORE_FIELDS.stream().anyMatch(getXMLPath()::contains)) {
      return;
    }

    currentValue.append(ch, start, length);
  }

  @Override
  public void endElement(final String uri, final String localName, final String qName) {
    if (!ConstantEventHashInfo.IGNORE_FIELDS.stream().anyMatch(getXMLPath()::contains)) {
      // Do not store the values for the fields which needs to be ignored such as EPCISDocument,
      // EPCISBody, etc.
      if (rootNode != null && !ConstantEventHashInfo.XML_IGNORE_FIELDS.contains(qName)) {
        // Call the method to write the appropriate values. Splitting the method to avoid the
        // cognitive complexity.
        xmlParser(qName);
      }

      // At the end of the every event in the document same the rootNode with the rootNodes and
      // assign
      // the rootNode with null for subsequent event.
      if (ConstantEventHashInfo.EPCIS_EVENT_TYPES.contains(qName)) {
        // After reading each XML event and converting it to ContextNode store the information in
        // rootNodes.
        emitter.emit(rootNode);

        // After creating the pre-hash string and generating Hash-ID discard the rootNode
        // information
        // for subsequent event.
        rootNode = null;
      }

      // At the end of the XML element tag reset the value for next element.
      currentValue.setLength(0);

      // After completing the particular element reading, remove that element from the stack.
      path.pop();
    } else if (ConstantEventHashInfo.IGNORE_FIELDS.stream().anyMatch(getXMLPath()::contains)) {
      path.pop();
    }
  }

  // Private method called by endElement to write the values. Splitting the method to avoid the
  // cognitive complexity.
  private void xmlParser(final String qName) {
    // Get the path from Deque as / separated values.
    final String p = getXMLPath();

    // Store the value of the current xml tag if available.
    final String value =
        !currentValue.toString().trim().equals("") ? currentValue.toString().trim() : null;

    // For complex fields add the values to its children
    if (ConstantEventHashInfo.WHAT_DIMENSION_XML_PATH.stream().anyMatch(p::startsWith)) {
      currentNode.children.add(new ContextNode(currentNode, path.peek(), value));
    } else {

      // If the current XML tag is part of WHY or HOW dimension then set the value for the
      // currentNode.
      if (ConstantEventHashInfo.WHY_DIMENSION_XML_PATH.stream().anyMatch(p::startsWith)
          || ConstantEventHashInfo.HOW_DIMENSION_XML_PATH.stream().anyMatch(p::startsWith)) {

        // If the attribute values are present within the XML then add them to attributes variable
        // in context node as childrens
        if (currentAttributes.size() > 0) {
          for (Map.Entry<String, String> attribute : currentAttributes.entrySet()) {
            currentNode.children.add(
                new ContextNode(currentNode, attribute.getKey(), attribute.getValue()));
          }
          currentNode.children.add(new ContextNode(currentNode, qName, value));
        }
        currentNode = currentNode.parent;
      } else if (currentNode != null) {
        currentNode.setValue(value);
        currentNode = currentNode.parent;
      }
    }
  }

  @Override
  public void endDocument() throws SAXException {
    super.endDocument();
    emitter.complete();
  }

  private String getXMLPath() {
    return String.join("/", this.path);
  }
}
