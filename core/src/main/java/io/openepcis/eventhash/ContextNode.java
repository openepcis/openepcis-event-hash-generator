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

import static io.openepcis.eventhash.constant.ConstantEventHashInfo.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.openepcis.constants.CBVVersion;
import io.openepcis.constants.EPCIS;
import io.openepcis.eventhash.constant.ConstantEventHashInfo;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import io.openepcis.identifiers.converter.util.ConverterUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class is utilized by EventHash and SaxHandler during the parsing of XML/JSON EPCIS document
 * to read the events. Event information are converted to ContextNode class form. The EPCIS event
 * information in the form of ContextNode are used for creating the pre-hash string by sorting and
 * modifying as per the EPCIS standard.
 */
@Getter
@Setter
@NoArgsConstructor
public class ContextNode {
  protected String name;
  protected String value;
  protected ArrayList<ContextNode> children = new ArrayList<>();
  protected ContextNode parent;
  protected Map<String, String> namespaces;

  // Constructor 1: To store the simple event field information such as type, eventTime, bizStep.
  public ContextNode(final ContextNode parent, final String name, final String value) {
    this.parent = parent;
    this.name = name;
    this.value = value;
    this.namespaces = parent.namespaces;
  }

  // Constructor 2: To store the complex field which has inner elements such as errorDeclaration,
  // readPoint.
  public ContextNode(
      final ContextNode parent,
      final String name,
      final Iterator<Map.Entry<String, JsonNode>> fields) {
    this(fields, parent.namespaces);
    this.parent = parent;
    this.name = name;
  }

  // Constructor 3: To store the objects contains within array such as SourceList, DestinationList.
  public ContextNode(final ContextNode parent, final Iterator<Map.Entry<String, JsonNode>> fields) {
    this(fields, parent.namespaces);
    this.parent = parent;
    this.namespaces = parent.namespaces;
  }

  // Constructor 4: To store the complex field which has elements within Array such as epcList,
  // childEPCs.
  public ContextNode(final ContextNode parent, final String name, final ArrayNode node) {
    this.parent = parent;
    this.name = name;
    this.namespaces = parent.namespaces;
    final Iterator<JsonNode> iterator = node.elements();

    // For event fields with values in Array, loop over the array and add the elements one by one to
    // child based on type of value.
    while (iterator.hasNext()) {
      var n = iterator.next();

      // If the array contains direct text value and not another array then get the textValue and
      // add it.
      if (n.isValueNode() && !n.isArray() && EPC_LISTS.stream().anyMatch(name::equals)) {
        children.add(new ContextNode(this, EPCIS.EPC, n.textValue()));
      } else if (n.isValueNode() && !n.isArray()) {
        children.add(new ContextNode(this, name, n.asText()));
      } else if (n.isArray()) {
        // If the array contains another array then add the values as arrayNode.
        final ArrayNode arrayNode = (ArrayNode) n;
        children.add(new ContextNode(this, name, arrayNode));
      } else if (n.isObject() && LIST_OF_OBJECTS.containsKey(name)) {
        // Omit storing the key twice during array of objects iteration, instead add the
        // corresponding string.
        children.add(new ContextNode(this, LIST_OF_OBJECTS.get(name), n.fields()));
      } else if (n.isObject() && EXCLUDE_LINE_BREAK.contains(name)) {
        // Omit storing the key twice during array of objects iteration and also do not add any
        // additional string.
        children.add(new ContextNode(this, n.fields()));
      } else if (n.isObject()) {
        // For extensions include the name
        children.add(new ContextNode(this, name, n.fields()));
      } else {
        // If the array contains again fields then get the fields and add it.
        children.add(new ContextNode(this, name, n.fields()));
      }
    }
  }

  // Constructor 5: Constructor called by the EventReader class to extract all event fields and
  // values
  public ContextNode(
      final Iterator<Map.Entry<String, JsonNode>> fields, final Map<String, String> namespaces) {
    this.namespaces = namespaces;

    while (fields.hasNext()) {
      var n = fields.next();

      // Ignore reading the fields which are not required for Event Pre-Hash
      if (ConstantEventHashInfo.getContext().getFieldsToExcludeInPrehash().contains(n.getKey())) {
        continue;
      }

      // For event fields with direct string value add to childrens directs by calling Constructor
      // 1. Eg: type, bizStep, disposition, etc.
      if (n.getValue().isValueNode() && !n.getValue().isArray()) {
        children.add(new ContextNode(this, n.getKey(), n.getValue().asText()));
      } else if (n.getValue().isArray()) {
        // For event fields with values in Array, convert them to ArrayNode then add the array to
        // children by calling Constructor 3. Eg: epcList, childEPCs, etc.
        final ArrayNode arrayNode = (ArrayNode) n.getValue();
        children.add(new ContextNode(this, n.getKey(), arrayNode));
      } else if (!n.getKey().equals(EPCIS.ERROR_DECLARATION)) {
        // For all other fields which may have complex structure, add the field values from it to
        // children by calling Constructor 2. Eg: readPoint, etc. but skip errorDeclaration
        children.add(new ContextNode(this, n.getKey(), n.getValue().fields()));
      }
    }
  }

  // Constructor 6: To store the namespaces during the reading of EPCIS XML document.
  public ContextNode(final Map<String, String> namespaces) {
    this.namespaces = namespaces;
  }

  private void sort(final Boolean standardFieldSort) {
    final HashNodeComparator comparator = new HashNodeComparator(this, standardFieldSort);

    // If childrens have values then sort them according to EPCIS standard
    if (!children.isEmpty()) {
      children.sort(comparator);
    }
  }

  // Method called by the external application after completion of converting the JSON/XML documents
  // into ContextNode.
  public String toShortenedString(final CBVVersion cbvVersion) {
    // For CBV 2.0: Add all the EPCIS standard fields to pre-hash string first then add all the
    // users extensions
    // field that can appear anywhere with event and append the created string to pre-hash string.
    if (CBVVersion.VERSION_2_0_0.equals(cbvVersion)) {
      return (epcisFieldsPreHashBuilder(cbvVersion)
              + String.join("", userExtensionsPreHashBuilder(cbvVersion)))
          .trim();
    } else {
      // For CBV 2.1: User Extensions that are part of standard fields are included within the
      // respective field
      return epcisFieldsPreHashBuilder(cbvVersion).trim();
    }
  }

  // Private method to return the Strings from well known EPCIS fields/attributes of EPCIS event
  // such as type, eventTime, bizStep etc. by omitting the User-Extensions.
  private String epcisFieldsPreHashBuilder(final CBVVersion cbvVersion) {
    // Check if the elements are of root elements and do not contain the children elements. If the
    // element is part of EPCIS standard fields then append to pre-hash string.
    if (children.isEmpty()
        && getName() != null
        && getValue() != null
        && TemplateNodeMap.isEpcisField(this)) {
      // If the elements are EPCIS event root fields then directly append them to the pre-hash
      // string by formatting.

      // Create the string for the attributes. If present append all attributes according EPCIS
      // standard. Used mainly for XML document.
      final StringBuilder preHashBuilder = new StringBuilder();

      // For ILMD fields make call to userExtensions formatter and for all other fields make call to
      // normal field formatter.
      if (Boolean.TRUE.equals(isIlmdPath(this))) {
        preHashBuilder.append(userExtensionsFormatter(name, value, namespaces));
      } else {
        // Add the values for direct name and value based on the field
        preHashBuilder.append(epcisFieldFormatter(getName(), getValue(), this));
      }

      return preHashBuilder.toString();
    } else if (children.isEmpty()
        && getName() != null
        && getValue() != null
        && !TemplateNodeMap.isEpcisField(this)
        && CBVVersion.VERSION_2_1_0.equals(cbvVersion)) {
      return userExtensionsFormatter(this.getName(), this.getValue(), this.getNamespaces());
    } else {
      final StringBuilder sb = new StringBuilder();

      // Call the function to add the EPCIS field name for children elements
      sb.append(fieldName(this, cbvVersion));

      // If child values are present then sort them according to event hash requirement
      this.sort(true);

      // After sorting the child values loop through each of them and add values to pre-hash string
      for (ContextNode node : children) {
        String s = "";
        if (node.getName() != null
            && !TemplateNodeMap.isEpcisField(node)
            && CBVVersion.VERSION_2_1_0.equals(cbvVersion)) {
          s = node.userExtensionsPreHashBuilder(cbvVersion);
        } else {
          s = node.epcisFieldsPreHashBuilder(cbvVersion);
        }
        if (!s.isEmpty()) {
          sb.append(s).append("\n");
        }
      }
      return sb.toString();
    }
  }

  // Private method to append the EPCIS field name during the child elements formatting.
  private String fieldName(final ContextNode node, final CBVVersion cbvVersion) {
    // For ILMD fields make call to userExtensions formatter and for all other fields make call to
    // normal field formatter.
    String fieldName = "";

    if (Boolean.TRUE.equals(isIlmdPath(node))) {
      if (!isArrayNode(node)) {
        fieldName = userExtensionsFormatter(node.getName(), node.getValue(), namespaces);
      }
    } else if (node.getName() != null
        && TemplateNodeMap.isEpcisField(node)
        && DUPLICATE_ENTRY_CHECK.stream().noneMatch(node.getName()::equals)
        && node.getChildren() != null
        && !node.getChildren().isEmpty()
        && node.getChildren().get(0).getName() != null
        && (!node.getName().equals(EPCIS.SENSOR_ELEMENT_LIST)
            || CBVVersion.VERSION_2_1_0.equals(cbvVersion))
        && (node.getName().equals(EPCIS.SENSOR_ELEMENT)
            || !node.getChildren().get(0).getName().equalsIgnoreCase(EPCIS.SENSOR_REPORT))) {
      // If the name does not contain null values & part of EPCIS standard fields then append to
      // pre-hash string. Additional condition has been added to avoid the addition of sensorReport
      // twice to the pre-hash string.
      fieldName = node.getName();
    } else if (node.getName() != null
        && TemplateNodeMap.isEpcisField(node)
        && node.getChildren() != null
        && !node.getChildren().isEmpty()
        && node.getChildren().get(0).getName() == null
        && ConstantEventHashInfo.getContext().getFieldsToExcludeInPrehash().stream()
            .noneMatch(getName()::equals)) {
      fieldName = node.getName();
    }

    return fieldName + "\n";
  }

  // Private function to store the path of the elements including the parents. Added to find the
  // ilmd elements and accordingly add the formatted ILMD elements
  protected Boolean isIlmdPath(final ContextNode node) {
    // Special handling for the ILMD fields as it contains User Extensions like elements but should
    // appear before User-Extensions as well known fields of EPCIS standard.
    final Deque<String> path = new ArrayDeque<>();

    path.push(node.getName() != null ? node.getName() : "");
    ContextNode fieldParent = node.getParent();
    while (fieldParent != null && fieldParent.getName() != null) {
      path.push(fieldParent.getName());
      fieldParent = fieldParent.getParent();
    }
    return path.contains(EPCIS.ILMD);
  }

  // private method to find the parent of the element which can be later used to convert the Bare
  // String in JSON format to Web URI format.
  private String findParent(final ContextNode node) {
    String parentFieldName = node.getName();
    ContextNode parentNode = node.getParent();

    while (parentNode != null) {
      parentFieldName =
          parentNode.getName() != null && !parentNode.getName().equals("")
              ? parentNode.getName()
              : parentFieldName;
      parentNode = parentNode.getParent();
    }
    return parentFieldName;
  }

  // Private method to return the List of Strings contains the  user-defined extensions in required
  // pre-hash format.
  private String userExtensionsPreHashBuilder(final CBVVersion cbvVersion) {
    // Create a string and append the values when the provided value is empty i.e. for complex
    // structures.
    StringBuilder sb = new StringBuilder();

    // Check for the fields which are not part of EPCIS standard fields and add them to the list
    if (children.isEmpty()
        && getName() != null
        && getValue() != null
        && (!TemplateNodeMap.isEpcisField(this) || TemplateNodeMap.addExtensionWrapperTag(this))
        && !ConstantEventHashInfo.getContext().getFieldsToExcludeInPrehash().contains(getName())
        && !findParent(this).equalsIgnoreCase(EPCIS.CONTEXT)) {
      // Add information related to direct name and value based fields. Then if attributes are
      // present then call the method to format them.
      return userExtensionsFormatter(name, value, namespaces) + "\n";
    } else {

      if (getName() != null
          && (!getName().equals(EPCIS.SENSOR_ELEMENT_LIST)
              || CBVVersion.VERSION_2_1_0.equals(cbvVersion))
          && (!TemplateNodeMap.isEpcisField(this) || TemplateNodeMap.addExtensionWrapperTag(this))
          && !ConstantEventHashInfo.getContext().getFieldsToExcludeInPrehash().contains(getName())
          && !findParent(this).equalsIgnoreCase(EPCIS.CONTEXT)
          && (getName().equals(EPCIS.SENSOR_ELEMENT)
              || (!children.isEmpty()
                  && children.get(0).getName() != null
                  && !getName().equals(getChildren().get(0).getName())
                  && !getChildren().get(0).getName().equalsIgnoreCase(EPCIS.SENSOR_REPORT)))) {
        sb.append(userExtensionsFormatter(getName(), getValue(), namespaces));
      }

      // Sort the children elements within the complex user extensions.
      this.sort(false);

      for (ContextNode node : children) {
        final String childExtension = node.userExtensionsPreHashBuilder(cbvVersion);
        if (!childExtension.isEmpty()) {
          sb.append(childExtension).append("\n");
        }
      }
      return sb.toString();
    }
  }

  // Event value formatter method to format the EPCIS event fields as per the event hash requirement
  // like to add substring or convert sub string.
  protected String epcisFieldFormatter(
      final String name, final String value, final ContextNode currentNode) {
    // If the field matches to ignore field then do not include them within the event pre hash. Ex:
    // recordTime
    if (ConstantEventHashInfo.getContext().getFieldsToExcludeInPrehash().stream()
        .anyMatch(name::startsWith)) {
      return null;
    }

    // For fields with name and value convert them to required WebURI format and suffix string if
    // required during pre-hash creation.
    if (EPC_LISTS.contains(name)) {
      // if instance identifiers are in URN format then change it to WebURI format
      if (value.startsWith(EPCIS.INSTANCE_IDENTIFIER_URN_PREFIX)) {
        return EPCIS.EPC + "=" + ConverterUtil.toURI(value);
      } else {
        return EPCIS.EPC + "=" + ConverterUtil.shortNameReplacer(value);
      }
    } else if ((value.startsWith(EPCIS.INSTANCE_IDENTIFIER_URN_PREFIX))
        || (CLASS_IDENTIFIER_URN_PREFIX.stream().anyMatch(value::startsWith))) {
      // If element value is in URN format then change it to WebURI format
      return name + "=" + gs1IdentifierFormat(value);
    } else if (SHORTNAME_FIELDS.stream().anyMatch(name::equals)) {
      // For instance/class identifier fields or sensor related fields replace the short names with
      // corresponding identifier keys and/or replace custom gs1 domain
      return name + "=" + ConverterUtil.shortNameReplacer(value);
    } else if ((name.equals(EPCIS.TYPE)
            || name.equals(EPCIS.EXCEPTION)
            || name.equals(EPCIS.COMPONENT))
        && (currentNode != null
            && currentNode.getParent() != null
            && currentNode.getParent().getName() != null
            && currentNode.getParent().getName().equals(EPCIS.SENSOR_REPORT))) {
      // For sensorReport type/exception field add the gs1 domain
      return formatSensorField(name, value);
    } else if (TIME_ATTRIBUTE_LIST.contains(name)) {
      // For all the date time information within the event convert the information to UTC time
      return name + "=" + DATE_FORMATTER.format(Instant.parse(value));
    } else if (GS1_ATTRIBUTES_PREFIX.stream().anyMatch(value::startsWith)) {
      // If the field is of bizStep, disposition, bizTransaction/source type then convert the URN to
      // WebURI vocabulary.
      return name + "=" + ConverterUtil.toWebURIVocabulary(value);
    } else if (currentNode != null
        && BARE_STRING_FIELD_PARENT_CHILD.containsKey(findParent(currentNode))
        && BARE_STRING_FIELD_PARENT_CHILD.get(findParent(currentNode)).stream()
            .anyMatch(name::equals)) {
      // If the field such as bizStep, disposition, bizTransactionList, sourceList, etc. contain the
      // bareString values then convert them to WebURI
      return name
          + "="
          + ConverterUtil.toCbvVocabulary(value, findParent(currentNode), EPCIS.WEBURI);
    } else if (SOURCE_DESTINATION_URN_PREFIX.stream().anyMatch(value::startsWith)) {
      // If the field is of Source/Destination SGLN type then convert the value from URN to WebURI.
      return name + "=" + ConverterUtil.toURI(value);
    } else if (value.startsWith(EPCIS.INSTANCE_IDENTIFIER_URN_PREFIX)
        || CLASS_IDENTIFIER_URN_PREFIX.stream().anyMatch(value::startsWith)) {
      // If the field is of Identifiers type then convert the value to WebURI type
      return name + "=" + ConverterUtil.toURI(value);
    } else if (value.startsWith(EPCIS.GS1_PREFIX)) {
      // For sensorReport elements if value contains gs1:Pressure etc. then strip the starting gs1:
      return name + "=" + value.substring(value.indexOf(EPCIS.GS1_PREFIX) + 4);
    } else if (EPCIS_EVENT_TYPES.stream().anyMatch(value::equals)) {
      // If the value matches any of the event type then replace the type with eventType to match
      // pre-hash string requirement
      return EPCIS.EVENT_TYPE + "=" + value + "\n";
    } else if (value.equals("")) {
      // If the field value has Null or empty values then return only the name. Used for sensor
      // information in XML document.
      return name;
    } else if (value.matches(DIGIT_CHECKER)) {
      // If value contains numbers then format them accordingly 25.0 -> 25, 25.6 -> 25.6 etc.
      return name + "=" + gs1IdentifierFormat(value);
    }
    return name + "=" + value;
  }

  // Method to format the values if it matches any of the GS1 identifiers format
  private String gs1IdentifierFormat(final String value) {
    if (value.startsWith(EPCIS.INSTANCE_IDENTIFIER_URN_PREFIX)) {
      // If element value is in URN format then change it to WebURI format
      return ConverterUtil.toURI(value);
    } else if (CLASS_IDENTIFIER_URN_PREFIX.stream().anyMatch(value::startsWith)) {
      // If quantity element class identifiers are in URN format then change it to WebURI format
      return ConverterUtil.toURIForClassLevelIdentifier(value);
    } else if (value.matches(DIGIT_CHECKER)) {
      // If value contains numbers then format them accordingly 25.0 -> 25, 25.6 -> 25.6 etc.
      final double interimValue = Double.parseDouble(value);
      if (interimValue % 1 == 0) {
        // If the value contains decimal 25.0 then return 25 if not then return same 25.2 to 25.2
        return Double.toString(interimValue).endsWith(".0")
            ? String.valueOf((int) Math.floor(interimValue))
            : value;
      } else {
        return value;
      }
    }
    return ConverterUtil.shortNameReplacer(value);
  }

  // Method to format sensor element fields such as type, exception
  private String formatSensorField(final String name, String value) {

    if (value.startsWith(EPCIS.GS1_PREFIX)) {
      value = SENSOR_REPORT_FORMAT.get(name) + value.substring(4);
    } else if (!value.contains(":")) {
      value = SENSOR_REPORT_FORMAT.get(name) + value;
    }
    return name + "=" + value;
  }

  // Method to find the namespace and return the respective string to calling function. Used during
  // the formatting of User Extensions.
  protected String userExtensionsFormatter(
      final String name, final String value, final Map<String, String> currentNamespaces) {
    // Check if the provided key contains the namespace if so then obtain the namespace else
    // namespace will be blank
    final String nameSpace =
        name != null && name.contains(":")
            ? currentNamespaces.get(name.substring(0, name.indexOf(":")))
            : null;

    // Based on namespace and value return the respective formatted User Extensions string
    if (nameSpace != null && value != null && !value.equals("")) {
      return "{"
          + nameSpace
          + "}"
          + name.substring(name.indexOf(":") + 1)
          + "="
          + gs1IdentifierFormat(value)
          + "\n";
    } else if (nameSpace != null) {
      return "{" + nameSpace + "}" + name.substring(name.indexOf(":") + 1);
    } else if (value != null && !value.equals("")) {
      return name + "=" + gs1IdentifierFormat(value) + "\n";
    } else {
      return name + "\n";
    }
  }

  // Check if the parent is array if not do not add the user extension namespace twice
  private boolean isArrayNode(final ContextNode node) {
    return node.getName() != null
        && !node.getChildren().isEmpty()
        && node.getChildren().get(0).getName() != null
        && (node.getName().equals(node.getChildren().get(0).getName())
            && node.getChildren().get(0).getValue() != null);
  }

  // Get the parent and their subsequent children (test purpose only)
  public String toString() {
    return name
        + ":"
        + Objects.requireNonNullElseGet(
            value,
            () ->
                children.stream()
                    .map(e -> e.getValue() + " " + String.join(","))
                    .collect(Collectors.toList()));
  }
}
