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

import static java.util.Map.entry;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

/**
 * Class containing some common information which are used in various conditions to convert the
 * XML/JSON EPCIS document into ContextNode object and while creating the pre-hash string.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConstantEventHashInfo {
  // Constant information required for reading, sorting, modification and conversion of the JSON/XML
  // EPCIS event field information.
  protected static final List<String> EPC_LISTS =
      List.of("epcList", "inputEPCList", "childEPCs", "outputEPCList");
  protected static final List<String> EVENT_TIME =
      List.of("eventTime", "declarationTime", "time", "startTime", "endTime");
  protected static final String CORRECTIVE_LIST = "correctiveEventIDs";
  protected static final String QUANTITY_ELEMENT = "quantityElement";
  protected static final String SENSOR_ELEMENT = "sensorElement";
  protected static final String SENSOR_REPORT = "sensorReport";
  protected static final String PERSISTENT_DISPOSITION = "persistentDisposition";
  protected static final String SENSOR_ELEMENT_LIST = "sensorElementList";
  protected static final String CONTEXT = "@context";
  protected static final List<String> DUPLICATE_ENTRY_CHECK = List.of("set", "unset");
  protected static final Map<String, String> LIST_OF_OBJECTS =
      Map.ofEntries(
          entry("quantityList", QUANTITY_ELEMENT),
          entry("childQuantityList", QUANTITY_ELEMENT),
          entry("inputQuantityList", QUANTITY_ELEMENT),
          entry("outputQuantityList", QUANTITY_ELEMENT),
          entry(SENSOR_ELEMENT_LIST, SENSOR_ELEMENT),
          entry(SENSOR_REPORT, SENSOR_REPORT));
  protected static final String INSTANCE_IDENTIFIER_URN_FORMAT = "urn:epc:id:";
  protected static final List<String> CLASS_IDENTIFIER_URN_FORMAT =
      List.of("urn:epc:class:", "urn:epc:idpat:");
  protected static final List<String> SOURCE_DESTINATION_URN_FORMAT =
      List.of("urn:epc:id:pgln:", "urn:epc:id:sgln:");
  protected static final List<String> CBV_STRING_TYPE =
      List.of(
          "urn:epcglobal:cbv:bizstep:",
          "urn:epcglobal:cbv:disp:",
          "urn:epcglobal:cbv:btt:",
          "urn:epcglobal:cbv:sdt:",
          "urn:epcglobal:cbv:er:");
  protected static final MultiValuedMap<String, String> BARE_STRING_FIELD_PARENT_CHILD =
      new ArrayListValuedHashMap<>();
  protected static final List<String> IGNORE_FIELDS = List.of("recordTime", "eventID", CONTEXT);
  protected static final DateTimeFormatter DATE_FORMATTER =
      new DateTimeFormatterBuilder().appendInstant(3).toFormatter();

  // Variables to read XML and store the relevant information within the Context Node.
  protected static final List<String> EPCIS_EVENT_TYPES =
      List.of(
          "ObjectEvent",
          "AggregationEvent",
          "TransactionEvent",
          "TransformationEvent",
          "AssociationEvent");
  protected static final List<String> XML_IGNORE_FIELDS =
      List.of("epcis:EPCISDocument", "EPCISBody", "EventList");
  protected static final List<String> WHAT_DIMENSION_XML_PATH =
      List.of("epc/epcList/", "epc/childEPCs/", "epc/inputEPCList/", "epc/outputEPCList");
  protected static final List<String> WHY_DIMENSION_XML_PATH =
      List.of(
          "bizTransaction/bizTransactionList", "source/sourceList", "destination/destinationList");
  protected static final List<String> HOW_DIMENSION_XML_PATH =
      List.of(
          "sensorMetadata/sensorElement/sensorElementList",
          "sensorReport/sensorElement/sensorElementList");
  protected static final List<String> USER_EXTENSION_WRAPPER =
      List.of(SENSOR_ELEMENT_LIST, SENSOR_ELEMENT);

  protected static final List<String> EXCLUDE_LINE_BREAK =
      List.of(
          "epcList",
          "childEPCs",
          "inputEPCList",
          "outputEPCList",
          "quantityList",
          "childQuantityList",
          "inputQuantityList",
          "outputQuantityList",
          QUANTITY_ELEMENT,
          "readPoint",
          "bizLocation",
          "bizTransactionList",
          "sourceList",
          "destinationList",
          "errorDeclaration",
          SENSOR_ELEMENT_LIST,
          SENSOR_ELEMENT,
          "sensorMetadata",
          SENSOR_REPORT,
          "type",
          PERSISTENT_DISPOSITION,
          "bizTransaction",
          "ilmd");

  protected static final List<String> SHORTNAME_FIELDS =
      List.of(
          "deviceID",
          "deviceMetadata",
          "rawData",
          "dataProcessingMethod",
          "bizRules",
          "microorganism",
          "chemicalSubstance",
          "coordinateReferenceSystem",
          "uriValue");

  static {
    BARE_STRING_FIELD_PARENT_CHILD.put("bizStep", "bizStep");
    BARE_STRING_FIELD_PARENT_CHILD.put("disposition", "disposition");
    BARE_STRING_FIELD_PARENT_CHILD.put("bizTransactionList", "type");
    BARE_STRING_FIELD_PARENT_CHILD.put(PERSISTENT_DISPOSITION, "set");
    BARE_STRING_FIELD_PARENT_CHILD.put(PERSISTENT_DISPOSITION, "unset");
    BARE_STRING_FIELD_PARENT_CHILD.put("sourceList", "type");
    BARE_STRING_FIELD_PARENT_CHILD.put("destinationList", "type");
    BARE_STRING_FIELD_PARENT_CHILD.put("errorDeclaration", "reason");
  }
}
