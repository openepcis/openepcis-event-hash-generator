/*
 * Copyright 2022-2026 benelog GmbH & Co. KG
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.openepcis.constants.CBVVersion;
import io.openepcis.constants.EPCIS;
import io.openepcis.eventhash.constant.ConstantEventHashInfo;
import io.openepcis.eventhash.util.PreHashStringGeneratorUtil;
import io.openepcis.identifiers.converter.util.ConverterUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static io.openepcis.eventhash.constant.ConstantEventHashInfo.*;

/**
 * This class is utilized by EventHash and SaxHandler during the parsing of XML/JSON EPCIS document to read the events. Event information are converted to ContextNode class form.
 * The EPCIS event information in the form of ContextNode are used for creating the pre-hash string by sorting and modifying as per the EPCIS standard.
 */
public class ContextNode {
    protected String name;
    protected String value;
    protected ArrayList<ContextNode> children = new ArrayList<>();
    protected ContextNode parent;
    protected Map<String, String> namespaces;
    // Fields omitted from the pre-hash string: the always-on defaults, optionally augmented per hash run.
    // Propagated unchanged to every child node so the whole event tree shares one exclusion view.
    protected Collection<String> fieldsToExclude = ConstantEventHashInfo.DEFAULT_FIELDS_TO_EXCLUDE_IN_PREHASH;

    public ContextNode() {
    }

    // Constructor 1: To store the simple event field information such as type, eventTime, bizStep.
    public ContextNode(final ContextNode parent, final String name, final String value) {
        this.parent = parent;
        this.name = name;
        this.value = value;
        this.namespaces = parent.namespaces;
        this.fieldsToExclude = parent.fieldsToExclude;
    }

    // Constructor 2: To store the complex field which has inner elements such as errorDeclaration, readPoint.
    public ContextNode(final ContextNode parent, final String name, final Iterator<Map.Entry<String, JsonNode>> fields) {
        this(fields, parent.namespaces, parent.fieldsToExclude);
        this.parent = parent;
        this.name = name;
    }

    // Constructor 3: To store the objects contains within array such as SourceList, DestinationList.
    public ContextNode(final ContextNode parent, final Iterator<Map.Entry<String, JsonNode>> fields) {
        this(fields, parent.namespaces, parent.fieldsToExclude);
        this.parent = parent;
        this.namespaces = parent.namespaces;
    }

    // Constructor 4: To store the complex field which has elements within Array such as epcList, childEPCs.
    public ContextNode(final ContextNode parent, final String name, final ArrayNode node) {
        this.parent = parent;
        this.name = name;
        this.namespaces = parent.namespaces;
        this.fieldsToExclude = parent.fieldsToExclude;
        final Iterator<JsonNode> iterator = node.elements();

        // For event fields with values in Array, loop over the array and add the elements one by one to child based on type of value.
        while (iterator.hasNext()) {
            var n = iterator.next();

            // If the array contains direct text value and not another array then get the textValue and add it.
            if (n.isValueNode() && !n.isArray() && EPC_LISTS.stream().anyMatch(name::equals)) {
                children.add(new ContextNode(this, EPCIS.EPC, n.textValue()));
            } else if (n.isValueNode() && !n.isArray()) {
                children.add(new ContextNode(this, name, n.asText()));
            } else if (n.isArray()) {
                // If the array contains another array then add the values as arrayNode.
                final ArrayNode arrayNode = (ArrayNode) n;
                children.add(new ContextNode(this, name, arrayNode));
            } else if (n.isObject() && LIST_OF_OBJECTS.containsKey(name)) {
                // Omit storing the key twice during array of objects iteration, instead add the corresponding string.
                children.add(new ContextNode(this, LIST_OF_OBJECTS.get(name), n.properties().iterator()));
            } else if (n.isObject() && EXCLUDE_LINE_BREAK.contains(name)) {
                // Omit storing the key twice during array of objects iteration and also do not add any additional string.
                children.add(new ContextNode(this, n.properties().iterator()));
            } else if (n.isObject()) {
                // For extensions include the name
                children.add(new ContextNode(this, name, n.properties().iterator()));
            } else {
                // If the array contains again fields then get the fields and add it.
                children.add(new ContextNode(this, name, n.properties().iterator()));
            }
        }
    }

    // Constructor 5: extract all event fields and values, excluding the default fields only.
    public ContextNode(final Iterator<Map.Entry<String, JsonNode>> fields, final Map<String, String> namespaces) {
        this(fields, namespaces, ConstantEventHashInfo.DEFAULT_FIELDS_TO_EXCLUDE_IN_PREHASH);
    }

    // Constructor 5b: as above, but with an explicit (default + per-run) set of fields to exclude.
    public ContextNode(final Iterator<Map.Entry<String, JsonNode>> fields, final Map<String, String> namespaces, final Collection<String> fieldsToExclude) {
        this.namespaces = namespaces;
        this.fieldsToExclude = fieldsToExclude;

        while (fields.hasNext()) {
            var n = fields.next();

            // Ignore reading the fields which are not required for Event Pre-Hash
            if (fieldsToExclude.contains(n.getKey())) {
                continue;
            }

            // For event fields with direct string value add to children directs by calling Constructor 1. Eg: type, bizStep, disposition, etc.
            if (n.getValue().isValueNode() && !n.getValue().isArray()) {
                children.add(new ContextNode(this, n.getKey(), n.getValue().asText()));
            } else if (n.getValue().isArray()) {
                // For event fields with values in Array, convert them to ArrayNode then add the array to children by calling Constructor 3. Eg: epcList, childEPCs, etc.
                final ArrayNode arrayNode = (ArrayNode) n.getValue();
                children.add(new ContextNode(this, n.getKey(), arrayNode));
            } else if (!n.getKey().equals(EPCIS.ERROR_DECLARATION)) {
                // all other fields which may have complex structure, add the field values from it to children via Constructor 2. Eg: readPoint, etc. skip errorDeclaration
                final JsonNode fieldValue = n.getValue();

                if (isAttributedLeaf(fieldValue)) {
                    // Attributed extension leaf: canonicalize to the XML/spec shape (value inline + attribute, no "@", no "value" token)
                    children.add(attributedLeafToContextNode(this, n.getKey(), fieldValue));
                } else {
                    // all other complex fields: unchanged behaviour (readPoint, nested extension objects, etc.)
                    children.add(new ContextNode(this, n.getKey(), n.getValue().properties().iterator()));
                }
            }
        }
    }

    // Constructor 6: To store the namespaces during the reading of EPCIS XML document.
    public ContextNode(final Map<String, String> namespaces) {
        this.namespaces = namespaces;
    }

    // Constructor 6b: as above, but carrying the (default + per-run) set of fields to exclude for the event tree.
    public ContextNode(final Map<String, String> namespaces, final Collection<String> fieldsToExclude) {
        this.namespaces = namespaces;
        this.fieldsToExclude = fieldsToExclude;
    }

    private void sort(final Boolean standardFieldSort) {
        final HashNodeComparator comparator = new HashNodeComparator(this, standardFieldSort);

        // If children have values then sort them according to EPCIS standard
        if (!children.isEmpty()) {
            children.sort(comparator);
        }
    }

    // Method called by the external application after completion of converting the JSON/XML documents into ContextNode.
    public String toShortenedString(final CBVVersion cbvVersion) {
        // Get the required way to generate pre-hash based on provided CBV version
        final CbvBehavior behavior = CbvBehavior.of(cbvVersion);

        if (behavior.inlineUserExtensions()) {
            // Inline behaviour (CBV 2.1+): user extensions are emitted alongside their standard fields.
            return epcisFieldsPreHashBuilder(behavior).trim();
        }

        // Two-pass behaviour (CBV 2.0): standard fields first, then a separate user-extensions pass.
        return (epcisFieldsPreHashBuilder(behavior) + String.join("", userExtensionsPreHashBuilder(behavior))).trim();
    }

    // Private method to return the Strings from well known EPCIS fields/attributes of EPCIS event such as type, eventTime, bizStep etc. by omitting the User-Extensions.
    // Dispatcher: classify this node as a leaf EPCIS field, an inlined user extension, or a parent — and route accordingly.
    private String epcisFieldsPreHashBuilder(final CbvBehavior behavior) {
        if (isLeafEpcisField()) {
            return formatLeafEpcisField();
        }
        if (isLeafUserExtensionInline(behavior)) {
            return userExtensionsFormatter(getName(), getValue(), getNamespaces());
        }
        return formatParentEpcisFields(behavior);
    }

    // Private method to append the EPCIS field name during the child elements formatting.
    private String fieldName(final ContextNode node, final CbvBehavior behavior) {
        // For ILMD fields make call to userExtensions formatter and for all other fields make call to normal field formatter.
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
                || behavior.keepSensorElementList())
                && (node.getName().equals(EPCIS.SENSOR_ELEMENT)
                || !node.getChildren().get(0).getName().equalsIgnoreCase(EPCIS.SENSOR_REPORT))) {
            // Append non-null EPCIS standard field names to the pre-hash string. Additional check prevents sensorReport from being added twice.
            fieldName = node.getName();
        } else if (node.getName() != null
                && TemplateNodeMap.isEpcisField(node)
                && node.getChildren() != null
                && !node.getChildren().isEmpty()
                && node.getChildren().get(0).getName() == null
                && fieldsToExclude.stream().noneMatch(getName()::equals)) {
            fieldName = node.getName();
        }

        return fieldName + "\n";
    }

    // Store the path of the elements including the parents. Added to find the ilmd elements and accordingly add the formatted ILMD elements
    protected Boolean isIlmdPath(final ContextNode node) {
        // Special handling for the ILMD fields as it contains User Extensions like elements but should appear before User-Extensions as well known fields of EPCIS standard.
        final Deque<String> path = new ArrayDeque<>();

        path.push(node.getName() != null ? node.getName() : "");
        ContextNode fieldParent = node.getParent();
        while (fieldParent != null && fieldParent.getName() != null) {
            path.push(fieldParent.getName());
            fieldParent = fieldParent.getParent();
        }
        return path.contains(EPCIS.ILMD);
    }

    // Find the parent of the element which can be later used to convert the Bare String in JSON format to Web URI format.
    private String findParent(final ContextNode node) {
        String parentFieldName = node.getName();
        ContextNode parentNode = node.getParent();

        while (parentNode != null) {
            parentFieldName = parentNode.getName() != null && !parentNode.getName().equals("") ? parentNode.getName() : parentFieldName;
            parentNode = parentNode.getParent();
        }
        return parentFieldName;
    }

    // To return the List of Strings contains the  user-defined extensions in required pre-hash format.
    // Dispatcher: leaf user-extension value → emit formatted; otherwise route through the parent formatter.
    private String userExtensionsPreHashBuilder(final CbvBehavior behavior) {
        if (isLeafUserExtension()) {
            return userExtensionsFormatter(name, value, namespaces) + "\n";
        }
        return formatParentUserExtensions(behavior);
    }

    // Event value formatter method to format the EPCIS event fields as per the event hash requirement like to add substring or convert sub string.
    protected String epcisFieldFormatter(final String name, final String value, final ContextNode currentNode) {
        // If the field matches to ignore field then do not include them within the event pre hash. Ex: recordTime
        if (fieldsToExclude.stream().anyMatch(name::startsWith)) {
            return null;
        }

        // For fields with name and value convert them to required WebURI format and suffix string if required during pre-hash creation.
        if (EPC_LISTS.contains(name)) {
            // if instance identifiers are in URN format then change it to WebURI format
            if (value.startsWith(EPCIS.INSTANCE_IDENTIFIER_URN_PREFIX)) {
                return EPCIS.EPC + "=" + PreHashStringGeneratorUtil.toUri(value);
            } else {
                return EPCIS.EPC + "=" + PreHashStringGeneratorUtil.shortName(value);
            }
        } else if ((value.startsWith(EPCIS.INSTANCE_IDENTIFIER_URN_PREFIX)) || (CLASS_IDENTIFIER_URN_PREFIX.stream().anyMatch(value::startsWith))) {
            // If element value is in URN format then change it to WebURI format
            return name + "=" + gs1IdentifierFormat(value);
        } else if (SHORTNAME_FIELDS.stream().anyMatch(name::equals)) {
            // For instance/class identifier fields or sensor related fields replace the short names with corresponding identifier keys and/or replace custom gs1 domain
            return name + "=" + PreHashStringGeneratorUtil.shortName(value);
        } else if ((name.equals(EPCIS.TYPE) || name.equals(EPCIS.EXCEPTION) || name.equals(EPCIS.COMPONENT))
                && (currentNode != null
                && currentNode.getParent() != null
                && currentNode.getParent().getName() != null
                && currentNode.getParent().getName().equals(EPCIS.SENSOR_REPORT))) {
            // For sensorReport type/exception field add the gs1 domain
            return formatSensorField(name, value);
        } else if (TIME_ATTRIBUTE_LIST.contains(name)) {
            // For all the date time information within the event convert the information to UTC time
            return name + "=" + PreHashStringGeneratorUtil.formatCanonicalTime(value);
        } else if (GS1_ATTRIBUTES_PREFIX.stream().anyMatch(value::startsWith)) {
            // If the field is of bizStep, disposition, bizTransaction/source type then convert the URN to WebURI vocabulary.
            return name + "=" + ConverterUtil.toWebURIVocabulary(value);
        } else if (currentNode != null
                && BARE_STRING_FIELD_PARENT_CHILD.containsKey(findParent(currentNode))
                && BARE_STRING_FIELD_PARENT_CHILD.get(findParent(currentNode)).stream().anyMatch(name::equals)) {
            // If the field such as bizStep, disposition, bizTransactionList, sourceList, etc. contain the bareString values then convert them to WebURI
            return name + "=" + ConverterUtil.toCbvVocabulary(value, findParent(currentNode), EPCIS.WEBURI);
        } else if (SOURCE_DESTINATION_URN_PREFIX.stream().anyMatch(value::startsWith)) {
            // If the field is of Source/Destination SGLN type then convert the value from URN to WebURI.
            return name + "=" + PreHashStringGeneratorUtil.toUri(value);
        } else if (value.startsWith(EPCIS.INSTANCE_IDENTIFIER_URN_PREFIX) || CLASS_IDENTIFIER_URN_PREFIX.stream().anyMatch(value::startsWith)) {
            // If the field is of Identifiers type then convert the value to WebURI type
            return name + "=" + PreHashStringGeneratorUtil.toUri(value);
        } else if (value.startsWith(EPCIS.GS1_PREFIX)) {
            // For sensorReport elements if value contains gs1:Pressure etc. then strip the starting gs1:
            return name + "=" + value.substring(value.indexOf(EPCIS.GS1_PREFIX) + 4);
        } else if (EPCIS_EVENT_TYPES.stream().anyMatch(value::equals)) {
            // If the value matches any of the event type then replace the type with eventType to match pre-hash string requirement
            return EPCIS.EVENT_TYPE + "=" + value + "\n";
        } else if (value.equals("")) {
            // If the field value has Null or empty values then return only the name. Used for sensor information in XML document.
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
            return PreHashStringGeneratorUtil.toUri(value);
        } else if (CLASS_IDENTIFIER_URN_PREFIX.stream().anyMatch(value::startsWith)) {
            // If quantity element class identifiers are in URN format then change it to WebURI format
            return PreHashStringGeneratorUtil.toUriClass(value);
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
        return PreHashStringGeneratorUtil.shortName(value);
    }

    // Method to format sensor element fields such as type, exception
    private String formatSensorField(final String name, String value) {
        if (value.startsWith(EPCIS.GS1_PREFIX)) {
            value = SENSOR_REPORT_FORMAT.get(name) + value.substring(4);
        } else if (value.startsWith(EPCIS.DEFAULT_CURIE_PREFIX)) {
            value = EPCIS.GS1_CBV_DOMAIN + value.substring(EPCIS.DEFAULT_CURIE_PREFIX.length());
        } else if (!value.contains(":")) {
            value = SENSOR_REPORT_FORMAT.get(name) + value;
        }
        return name + "=" + value;
    }

    // Method to find the namespace and return the respective string to calling function. Used during the formatting of User Extensions.
    protected String userExtensionsFormatter(final String name, final String value, final Map<String, String> currentNamespaces) {
        // Check if the provided key contains the namespace if so then obtain the namespace else namespace will be blank
        final String nameSpace = name != null && name.contains(":") ? currentNamespaces.get(name.substring(0, name.indexOf(":"))) : null;

        // Based on namespace and value return the respective formatted User Extensions string
        if (nameSpace != null && value != null && !value.equals("")) {
            return "{" + nameSpace + "}" + name.substring(name.indexOf(":") + 1) + "=" + gs1IdentifierFormat(value) + "\n";
        } else if (nameSpace != null) {
            return "{" + nameSpace + "}" + name.substring(name.indexOf(":") + 1) + "\n";
        } else if (value != null && !value.equals("")) {
            return name + "=" + gs1IdentifierFormat(value) + "\n";
        } else {
            return name + "\n";
        }
    }

    // Check if the parent is array if not do not add the user extension namespace twice
    private boolean isArrayNode(final ContextNode node) {
        if (node.getName() == null || node.getChildren().isEmpty()) return false;

        long sameName = node.getChildren().stream().filter(c -> node.getName().equals(c.getName())).count();
        if (sameName >= 2) return true; // 2+ same-name children => JSON array wrapper (scalars or objects)
        if (sameName == 0) return false; // not a wrapper

        // exactly one same-name child: array-of-one-scalar has a value; a nested same-name object does not
        final ContextNode only = node.getChildren().stream().filter(c -> node.getName().equals(c.getName())).findFirst().get();
        return only.getValue() != null;
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

    // True when this is a no-children value node that maps to a standard EPCIS field per TemplateNodeMap.
    private boolean isLeafEpcisField() {
        return children.isEmpty() && getName() != null && getValue() != null && TemplateNodeMap.isEpcisField(this);
    }

    // True when this is a no-children user-extension value, inlined alongside standard fields (CBV 2.1+).
    private boolean isLeafUserExtensionInline(final CbvBehavior cbvBehavior) {
        return children.isEmpty() && getName() != null && getValue() != null && !TemplateNodeMap.isEpcisField(this) && cbvBehavior.inlineUserExtensions();
    }

    // Format a leaf EPCIS field; ILMD fields go through the user-extensions formatter, everything else through epcisFieldFormatter.
    String formatLeafEpcisField() {
        return Boolean.TRUE.equals(isIlmdPath(this)) ? userExtensionsFormatter(name, value, namespaces) : epcisFieldFormatter(getName(), getValue(), this);
    }

    // Canonical, post-normalization key used for ordering, sort identifiers after the Canonical
    String canonicalComparisonKey() {
        final String formatted = formatLeafEpcisField();
        return StringUtils.isNotBlank(formatted) ? formatted : (StringUtils.isNotBlank(getName()) ? getValue() : getName());
    }

    // Emit the parent's name, sort its children, then recursively append each child's contribution.
    private String formatParentEpcisFields(final CbvBehavior cbvBehavior) {
        final StringBuilder sb = new StringBuilder();
        sb.append(fieldName(this, cbvBehavior));
        sort(true);

        for (final ContextNode child : children) {
            final String contribution = child.childContributionToEpcisStream(cbvBehavior);
            if (!contribution.isEmpty()) {
                sb.append(contribution).append("\n");
            }
        }

        return sb.toString();
    }

    // Per-child dispatch: is this child a user extension (inlined under CBV 2.1+) or a normal EPCIS field?
    private String childContributionToEpcisStream(final CbvBehavior behavior) {
        if (getName() != null && !TemplateNodeMap.isEpcisField(this) && behavior.inlineUserExtensions()) {
            return userExtensionsPreHashBuilder(behavior);
        }
        return epcisFieldsPreHashBuilder(behavior);
    }

    // True when this is a no-children value-bearing user extension, not in an excluded path and not under @context.
    private boolean isLeafUserExtension() {
        return children.isEmpty()
                && getName() != null
                && getValue() != null
                && (!TemplateNodeMap.isEpcisField(this) || TemplateNodeMap.addExtensionWrapperTag(this))
                && !fieldsToExclude.contains(getName())
                && !findParent(this).equalsIgnoreCase(EPCIS.CONTEXT);
    }


    // Parent user-extension formatting: optionally emit this node's wrapper tag, sort children, then recurse.
    private String formatParentUserExtensions(final CbvBehavior behavior) {
        final StringBuilder sb = new StringBuilder();

        if (shouldEmitOwnUserExtensionTag(behavior)) {
            sb.append(userExtensionsFormatter(getName(), getValue(), namespaces));
        }
        sort(false);

        for (final ContextNode child : children) {
            final String childExtension = child.userExtensionsPreHashBuilder(behavior);
            if (!childExtension.isEmpty()) {
                sb.append(childExtension).append("\n");
            }
        }
        return sb.toString();
    }

    // Predicate for emitting this node's own wrapper tag in the user-extension (sensorElement / non-EPCIS / non-excluded / not-under-@context / not-a-bare-list-around-sensorReport).
    private boolean shouldEmitOwnUserExtensionTag(final CbvBehavior behavior) {
        return getName() != null
                && (!getName().equals(EPCIS.SENSOR_ELEMENT_LIST) || behavior.keepSensorElementList())
                && (!TemplateNodeMap.isEpcisField(this) || TemplateNodeMap.addExtensionWrapperTag(this))
                && !fieldsToExclude.contains(getName())
                && !findParent(this).equalsIgnoreCase(EPCIS.CONTEXT)
                && (getName().equals(EPCIS.SENSOR_ELEMENT)
                || (!children.isEmpty()
                && children.get(0).getName() != null
                && !isArrayNode(this)
                && !getChildren().get(0).getName().equalsIgnoreCase(EPCIS.SENSOR_REPORT)));
    }

    /**
     * A JSON "attributed leaf" is how an XML element with attributes + simple text is encoded: e.g. {"@measurementUnitCode":"KGM","value":"3.5"}
     * exactly one "value" key (the text) plus zero or more "@"-prefixed attribute keys, and nothing else.
     **/
    public static boolean isAttributedLeaf(final JsonNode obj) {
        if (obj == null || !obj.isObject() || !obj.has("value")) {
            return false;
        }

        final Iterator<String> keys = obj.fieldNames();
        while (keys.hasNext()) {
            final String key = keys.next();

            // a non-attribute, non-value key -> not a leaf (e.g. sensorReport)
            if (!key.equals("value") && !key.startsWith("@")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Build the same ContextNode shape the XML SaxHandler produces for such an element:
     * the element carries its value inline ({ns}name=3.5), attributes become children with the JSON "@" stripped.
     */
    public static ContextNode attributedLeafToContextNode(final ContextNode parent, final String name, final JsonNode obj) {
        // value inline -> {ns}drainedWeight=3.5
        final ContextNode node = new ContextNode(parent, name, obj.get("value").asText());

        // add attributes as children
        for (final Map.Entry<String, JsonNode> attr : obj.properties()) {
            final String attrKey = attr.getKey();
            if (attrKey.startsWith("@")) {
                node.getChildren().add(new ContextNode(node, attrKey.substring(1), attr.getValue().asText()));
            }
        }

        return node;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public ArrayList<ContextNode> getChildren() {
        return children;
    }

    public void setChildren(final ArrayList<ContextNode> children) {
        this.children = children;
    }

    public ContextNode getParent() {
        return parent;
    }

    public void setParent(final ContextNode parent) {
        this.parent = parent;
    }

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(final Map<String, String> namespaces) {
        this.namespaces = namespaces;
    }

    public Collection<String> getFieldsToExclude() {
        return fieldsToExclude;
    }

    public void setFieldsToExclude(final Collection<String> fieldsToExclude) {
        this.fieldsToExclude = fieldsToExclude;
    }
}
