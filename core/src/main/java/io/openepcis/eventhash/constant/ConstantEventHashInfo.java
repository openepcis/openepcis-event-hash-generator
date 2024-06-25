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
package io.openepcis.eventhash.constant;

import static java.util.Map.entry;

import io.openepcis.constants.EPCIS;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
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
      List.of(EPCIS.EPC_LIST, EPCIS.CHILD_EPCS, EPCIS.INPUT_EPC_LIST, EPCIS.OUTPUT_EPC_LIST);
  public static final List<String> TIME_ATTRIBUTE_LIST =
      List.of(EPCIS.EVENT_TIME, EPCIS.TIME, EPCIS.START_TIME, EPCIS.END_TIME);
  public static final String DIGIT_CHECKER = "^-?\\d+(\\.\\d+)?$";
  public static final List<String> DUPLICATE_ENTRY_CHECK = List.of(EPCIS.SET, EPCIS.UNSET);
  public static final Map<String, String> LIST_OF_OBJECTS =
      Map.ofEntries(
          entry(EPCIS.QUANTITY_LIST, EPCIS.QUANTITY_ELEMENT),
          entry(EPCIS.CHILD_QUANTITY_LIST, EPCIS.QUANTITY_ELEMENT),
          entry(EPCIS.INPUT_QUANTITY_LIST, EPCIS.QUANTITY_ELEMENT),
          entry(EPCIS.OUTPUT_QUANTITY_LIST, EPCIS.QUANTITY_ELEMENT),
          entry(EPCIS.SENSOR_ELEMENT_LIST, EPCIS.SENSOR_ELEMENT),
          entry(EPCIS.SENSOR_REPORT, EPCIS.SENSOR_REPORT));
  public static final List<String> CLASS_IDENTIFIER_URN_PREFIX =
      List.of(
          EPCIS.CLASS_IDENTIFIER_URN_PREFIX_WITH_CLASS,
          EPCIS.CLASS_IDENTIFIER_URN_PREFIX_WITH_IDPAT);
  public static final List<String> SOURCE_DESTINATION_URN_PREFIX =
      List.of(EPCIS.PARTY_GLN_IDENTIFIER_URN_PREFIX, EPCIS.SERIALIZED_GLN_IDENTIFIER_URN_PREFIX);
  public static final List<String> GS1_ATTRIBUTES_PREFIX =
      List.of(
          EPCIS.BIZ_STEP_URN_PREFIX,
          EPCIS.DISPOSITION_URN_PREFIX,
          EPCIS.BIZ_TRANSACTION_URN_PREFIX,
          EPCIS.SRC_DEST_URN_PREFIX,
          EPCIS.ERROR_REASON_URN_PREFIX);
  public static final List<String> DEFAULT_FIELDS_TO_EXCLUDE_IN_PREHASH =
      new ArrayList<>(
          List.of(
              EPCIS.ERROR_DECLARATION,
              EPCIS.DECLARATION_TIME,
              EPCIS.REASON,
              EPCIS.CORRECTIVE_EVENT_IDS,
              EPCIS.CORRECTIVE_EVENT_ID,
              EPCIS.RECORD_TIME,
              EPCIS.EVENT_ID,
              EPCIS.CONTEXT,
              "rdfs:comment",
              "#text",
              "comment"));
  public static final DateTimeFormatter DATE_FORMATTER =
      new DateTimeFormatterBuilder().appendInstant(3).toFormatter();

  // Variables to read XML and store the relevant information within the Context Node.
  public static final List<String> EPCIS_EVENT_TYPES =
      List.of(
          EPCIS.OBJECT_EVENT,
          EPCIS.AGGREGATION_EVENT,
          EPCIS.TRANSACTION_EVENT,
          EPCIS.TRANSFORMATION_EVENT,
          EPCIS.ASSOCIATION_EVENT);
  public static final List<String> EXCLUDE_XML_FIELDS =
      List.of(
          EPCIS.EPCIS_DOCUMENT_WITH_NAMESPACE,
          EPCIS.EPCIS_BODY_IN_CAMEL_CASE,
          EPCIS.EVENT_LIST_IN_CAMEL_CASE);
  public static final List<String> WHAT_DIMENSION_XML_PATH =
      List.of(
          EPCIS.EPC + EPCIS.PATH_DELIMITER + EPCIS.EPC_LIST + EPCIS.PATH_DELIMITER,
          EPCIS.EPC + EPCIS.PATH_DELIMITER + EPCIS.CHILD_EPCS + EPCIS.PATH_DELIMITER,
          EPCIS.EPC + EPCIS.PATH_DELIMITER + EPCIS.INPUT_EPC_LIST + EPCIS.PATH_DELIMITER,
          EPCIS.EPC + EPCIS.PATH_DELIMITER + EPCIS.OUTPUT_EPC_LIST + EPCIS.PATH_DELIMITER);
  public static final List<String> WHY_DIMENSION_XML_PATH =
      List.of(
          EPCIS.BIZ_TRANSACTION + EPCIS.PATH_DELIMITER + EPCIS.BIZ_TRANSACTION_LIST,
          EPCIS.SOURCE + EPCIS.PATH_DELIMITER + EPCIS.SOURCE_LIST,
          EPCIS.DESTINATION + EPCIS.PATH_DELIMITER + EPCIS.DESTINATION_LIST);
  public static final List<String> HOW_DIMENSION_XML_PATH =
      List.of(
          EPCIS.SENSOR_METADATA
              + EPCIS.PATH_DELIMITER
              + EPCIS.SENSOR_ELEMENT
              + EPCIS.PATH_DELIMITER
              + EPCIS.SENSOR_ELEMENT_LIST,
          EPCIS.SENSOR_REPORT
              + EPCIS.PATH_DELIMITER
              + EPCIS.SENSOR_ELEMENT
              + EPCIS.PATH_DELIMITER
              + EPCIS.SENSOR_ELEMENT_LIST);
  public static final List<String> USER_EXTENSION_WRAPPER =
      List.of(EPCIS.SENSOR_ELEMENT_LIST, EPCIS.SENSOR_ELEMENT);

  public static final List<String> EXCLUDE_LINE_BREAK =
      List.of(
          EPCIS.EPC_LIST,
          EPCIS.CHILD_EPCS,
          EPCIS.INPUT_EPC_LIST,
          EPCIS.OUTPUT_EPC_LIST,
          EPCIS.OUTPUT_QUANTITY_LIST,
          EPCIS.CHILD_QUANTITY_LIST,
          EPCIS.INPUT_QUANTITY_LIST,
          EPCIS.OUTPUT_QUANTITY_LIST,
          EPCIS.QUANTITY_ELEMENT,
          EPCIS.READ_POINT,
          EPCIS.BIZ_LOCATION,
          EPCIS.BIZ_TRANSACTION_LIST,
          EPCIS.SOURCE_LIST,
          EPCIS.DESTINATION_LIST,
          EPCIS.SENSOR_ELEMENT_LIST,
          EPCIS.SENSOR_ELEMENT,
          EPCIS.SENSOR_METADATA,
          EPCIS.SENSOR_REPORT,
          EPCIS.TYPE,
          EPCIS.PERSISTENT_DISPOSITION,
          EPCIS.BIZ_TRANSACTION,
          EPCIS.ILMD);

  public static final List<String> SHORTNAME_FIELDS =
      List.of(
          EPCIS.EPC,
          EPCIS.EPC_CLASS,
          EPCIS.ID,
          EPCIS.DEVICE_ID,
          EPCIS.DEVICE_META_DATA,
          EPCIS.RAW_DATA,
          EPCIS.DATA_PROCESSING_METHOD,
          EPCIS.BIZ_RULES,
          EPCIS.MICROORGANISM,
          EPCIS.CHEMICAL_SUBSTANCE,
          EPCIS.COORDINATE_REFERENCE_SYSTEM,
          EPCIS.URI_VALUE);
  public static final Map<String, String> SENSOR_REPORT_FORMAT = new HashMap<>();
  public static final MultiValuedMap<String, String> BARE_STRING_FIELD_PARENT_CHILD =
      new ArrayListValuedHashMap<>();

  public final List<String> FIELDS_TO_EXCLUDE_IN_PREHASH =
      new ArrayList<>(DEFAULT_FIELDS_TO_EXCLUDE_IN_PREHASH);

  static {
    BARE_STRING_FIELD_PARENT_CHILD.put(EPCIS.BIZ_STEP, EPCIS.BIZ_STEP);
    BARE_STRING_FIELD_PARENT_CHILD.put(EPCIS.DISPOSITION, EPCIS.DISPOSITION);
    BARE_STRING_FIELD_PARENT_CHILD.put(EPCIS.BIZ_TRANSACTION_LIST, EPCIS.TYPE);
    BARE_STRING_FIELD_PARENT_CHILD.put(EPCIS.PERSISTENT_DISPOSITION, EPCIS.SET);
    BARE_STRING_FIELD_PARENT_CHILD.put(EPCIS.PERSISTENT_DISPOSITION, EPCIS.UNSET);
    BARE_STRING_FIELD_PARENT_CHILD.put(EPCIS.SOURCE_LIST, EPCIS.TYPE);
    BARE_STRING_FIELD_PARENT_CHILD.put(EPCIS.DESTINATION_LIST, EPCIS.TYPE);

    SENSOR_REPORT_FORMAT.put(EPCIS.TYPE, EPCIS.GS1_VOC_DOMAIN);
    SENSOR_REPORT_FORMAT.put(EPCIS.EXCEPTION, EPCIS.GS1_VOC_DOMAIN);
    SENSOR_REPORT_FORMAT.put(EPCIS.COMPONENT, EPCIS.GS1_CBV_DOMAIN + "Comp-");
  }

  private static final ConstantEventHashInfo context = new ConstantEventHashInfo();

  public static ConstantEventHashInfo getContext() {
    return context;
  }

  public void addFieldsToExclude(final List<String> fieldsToExclude) {
    // Add all the default fields that's not required in event-hash
    FIELDS_TO_EXCLUDE_IN_PREHASH.addAll(DEFAULT_FIELDS_TO_EXCLUDE_IN_PREHASH);

    // Add the user provided fields which needs to be excluded
    if (fieldsToExclude != null && !fieldsToExclude.isEmpty()) {
      FIELDS_TO_EXCLUDE_IN_PREHASH.addAll(fieldsToExclude);
    }
  }

  public void clearFieldsToExclude() {
    FIELDS_TO_EXCLUDE_IN_PREHASH.clear();
  }

  public List<String> getFieldsToExcludeInPrehash() {
    return FIELDS_TO_EXCLUDE_IN_PREHASH;
  }
}
