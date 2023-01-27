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
package io.openepcis.epc.eventhash.constant;

import static io.openepcis.epc.eventhash.constant.ConstantEPCISInfo.*;
import static java.util.Map.entry;

import io.openepcis.epc.translator.constants.Constants;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
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
  public static final List<String> EPC_LISTS =
      List.of(EPC_LIST, CHILD_EPCS, INPUT_EPC_LIST, OUTPUT_EPC_LIST);
  public static final List<String> TIME_ATTRIBUTE_LIST =
      List.of(EVENT_TIME, TIME, START_TIME, END_TIME);
  public static final String DIGIT_CHECKER = "^-?\\d+(\\.\\d+)?$";
  public static final List<String> DUPLICATE_ENTRY_CHECK = List.of(SET, UNSET);
  public static final Map<String, String> LIST_OF_OBJECTS =
      Map.ofEntries(
          entry(QUANTITY_LIST, QUANTITY_ELEMENT),
          entry(CHILD_QUANTITY_LIST, QUANTITY_ELEMENT),
          entry(INPUT_QUANTITY_LIST, QUANTITY_ELEMENT),
          entry(OUTPUT_QUANTITY_LIST, QUANTITY_ELEMENT),
          entry(SENSOR_ELEMENT_LIST, SENSOR_ELEMENT),
          entry(SENSOR_REPORT, SENSOR_REPORT));
  public static final List<String> CLASS_IDENTIFIER_URN_PREFIX =
      List.of(CLASS_IDENTIFIER_URN_PREFIX_WITH_CLASS, CLASS_IDENTIFIER_URN_PREFIX_WITH_IDPAT);
  public static final List<String> SOURCE_DESTINATION_URN_PREFIX =
      List.of(PARTY_GLN_IDENTIFIER_URN_PREFIX, SERIALIZED_GLN_IDENTIFIER_URN_PREFIX);
  public static final List<String> GS1_ATTRIBUTES_PREFIX =
      List.of(
          BIZ_STEP_EPC_URN_PREFIX,
          DISPOSITION_EPC_URN_PREFIX,
          BIZ_TRANSACTION_EPC_URN_PREFIX,
          SOURCE_DESTINATION_EPC_URN_PREFIX,
          ERROR_REASON_EPC_URN_PREFIX);
  public static final List<String> EXCLUDE_FIELDS_IN_PREHASH =
      List.of(
          ERROR_DECLARATION,
          DECLARATION_TIME,
          REASON,
          CORRECTIVE_EVENT_IDS,
          CORRECTIVE_EVENT_ID,
          RECORD_TIME,
          EVENT_ID,
          CONTEXT,
          "rdfs:comment",
          "#text",
          "comment");
  public static final DateTimeFormatter DATE_FORMATTER =
      new DateTimeFormatterBuilder().appendInstant(3).toFormatter();

  // Variables to read XML and store the relevant information within the Context Node.
  public static final List<String> EPCIS_EVENT_TYPES =
      List.of(
          OBJECT_EVENT,
          AGGREGATION_EVENT,
          TRANSACTION_EVENT,
          TRANSFORMATION_EVENT,
          ASSOCIATION_EVENT);
  public static final List<String> EXCLUDE_XML_FIELDS =
      List.of(EPCIS_DOCUMENT_WITH_NAMESPACE, EPCIS_BODY, EPCIS_EVENTS_LIST);
  public static final List<String> WHAT_DIMENSION_XML_PATH =
      List.of(
          EPC + PATH_DELIMITER + EPC_LIST + PATH_DELIMITER,
          EPC + PATH_DELIMITER + CHILD_EPCS + PATH_DELIMITER,
          EPC + PATH_DELIMITER + INPUT_EPC_LIST + PATH_DELIMITER,
          EPC + PATH_DELIMITER + OUTPUT_EPC_LIST + PATH_DELIMITER);
  public static final List<String> WHY_DIMENSION_XML_PATH =
      List.of(
          BIZ_TRANSACTION + PATH_DELIMITER + BIZ_TRANSACTION_LIST,
          SOURCE + PATH_DELIMITER + SOURCE_LIST,
          DESTINATION + PATH_DELIMITER + DESTINATION_LIST);
  public static final List<String> HOW_DIMENSION_XML_PATH =
      List.of(
          SENSOR_METADATA + PATH_DELIMITER + SENSOR_ELEMENT + PATH_DELIMITER + SENSOR_ELEMENT_LIST,
          SENSOR_REPORT + PATH_DELIMITER + SENSOR_ELEMENT + PATH_DELIMITER + SENSOR_ELEMENT_LIST);
  public static final List<String> USER_EXTENSION_WRAPPER =
      List.of(SENSOR_ELEMENT_LIST, SENSOR_ELEMENT);

  public static final List<String> EXCLUDE_LINE_BREAK =
      List.of(
          EPC_LIST,
          CHILD_EPCS,
          INPUT_EPC_LIST,
          OUTPUT_EPC_LIST,
          OUTPUT_QUANTITY_LIST,
          CHILD_QUANTITY_LIST,
          INPUT_QUANTITY_LIST,
          OUTPUT_QUANTITY_LIST,
          QUANTITY_ELEMENT,
          READ_POINT,
          BIZ_LOCATION,
          BIZ_TRANSACTION_LIST,
          SOURCE_LIST,
          DESTINATION_LIST,
          SENSOR_ELEMENT_LIST,
          SENSOR_ELEMENT,
          SENSOR_METADATA,
          SENSOR_REPORT,
          TYPE,
          PERSISTENT_DISPOSITION,
          BIZ_TRANSACTION,
          ILMD);

  public static final List<String> SHORTNAME_FIELDS =
      List.of(
          EPC,
          EPC_CLASS,
          ID,
          DEVICE_ID,
          DEVICE_META_DATA,
          RAW_DATA,
          DATA_PROCESSING_METHOD,
          BIZ_RULES,
          MICROORGANISM,
          CHEMICAL_SUBSTANCE,
          COORDINATE_REFERENCE_SYSTEM,
          URI_VALUE);
  public static final Map<String, String> SENSOR_REPORT_FORMAT = new HashMap<>();
  public static final MultiValuedMap<String, String> BARE_STRING_FIELD_PARENT_CHILD =
      new ArrayListValuedHashMap<>();

  static {
    BARE_STRING_FIELD_PARENT_CHILD.put(BIZ_STEP, BIZ_STEP);
    BARE_STRING_FIELD_PARENT_CHILD.put(DISPOSITION, DISPOSITION);
    BARE_STRING_FIELD_PARENT_CHILD.put(BIZ_TRANSACTION_LIST, TYPE);
    BARE_STRING_FIELD_PARENT_CHILD.put(PERSISTENT_DISPOSITION, SET);
    BARE_STRING_FIELD_PARENT_CHILD.put(PERSISTENT_DISPOSITION, UNSET);
    BARE_STRING_FIELD_PARENT_CHILD.put(SOURCE_LIST, TYPE);
    BARE_STRING_FIELD_PARENT_CHILD.put(DESTINATION_LIST, TYPE);

    SENSOR_REPORT_FORMAT.put(TYPE, Constants.GS1_VOC_DOMAIN);
    SENSOR_REPORT_FORMAT.put(EXCEPTION, Constants.GS1_VOC_DOMAIN);
    SENSOR_REPORT_FORMAT.put(COMPONENT, Constants.GS1_CBV_DOMAIN + "Comp-");
  }
}
