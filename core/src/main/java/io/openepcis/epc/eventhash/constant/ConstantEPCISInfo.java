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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConstantEPCISInfo {

  // Basic event info
  public static final String TYPE = "type";
  public static final String EVENT_TYPE = "eventType";
  public static final String OBJECT_EVENT = "ObjectEvent";
  public static final String AGGREGATION_EVENT = "AggregationEvent";
  public static final String TRANSACTION_EVENT = "TransactionEvent";
  public static final String TRANSFORMATION_EVENT = "TransformationEvent";
  public static final String ASSOCIATION_EVENT = "AssociationEvent";

  // WHAT dimension info
  public static final String PARENT_ID = "parentID";
  public static final String EPC_LIST = "epcList";
  public static final String CHILD_EPCS = "childEPCs";
  public static final String INPUT_EPC_LIST = "inputEPCList";
  public static final String OUTPUT_EPC_LIST = "outputEPCList";
  public static final String QUANTITY_LIST = "quantityList";
  public static final String CHILD_QUANTITY_LIST = "childQuantityList";
  public static final String INPUT_QUANTITY_LIST = "inputQuantityList";
  public static final String OUTPUT_QUANTITY_LIST = "outputQuantityList";
  public static final String EPC = "epc";
  public static final String QUANTITY_ELEMENT = "quantityElement";
  public static final String EPC_CLASS = "epcClass";
  public static final String QUANTITY = "quantity";
  public static final String UOM = "uom";

  // WHEN dimension info
  public static final String EVENT_TIME = "eventTime";
  public static final String RECORD_TIME = "recordTime";
  public static final String EVENT_TIME_ZONE_OFFSET = "eventTimeZoneOffset";

  // WHERE dimension info
  public static final String READ_POINT = "readPoint";
  public static final String BIZ_LOCATION = "bizLocation";
  public static final String ID = "id";

  // WHY dimension info
  public static final String BIZ_STEP = "bizStep";
  public static final String DISPOSITION = "disposition";
  public static final String BIZ_TRANSACTION_LIST = "bizTransactionList";
  public static final String BIZ_TRANSACTION = "bizTransaction";
  public static final String SOURCE_LIST = "sourceList";
  public static final String SOURCE = "source";
  public static final String DESTINATION_LIST = "destinationList";
  public static final String DESTINATION = "destination";
  public static final String PERSISTENT_DISPOSITION = "persistentDisposition";
  public static final String SET = "set";
  public static final String UNSET = "unset";
  public static final String ILMD = "ilmd";
  public static final String EXTENSION = "extension";

  // How dimension info
  public static final String SENSOR_ELEMENT_LIST = "sensorElementList";
  public static final String SENSOR_ELEMENT = "sensorElement";
  public static final String SENSOR_METADATA = "sensorMetadata";
  public static final String SENSOR_REPORT = "sensorReport";

  // Sensor Metadata info
  public static final String TIME = "time";
  public static final String START_TIME = "startTime";
  public static final String END_TIME = "endTime";
  public static final String DEVICE_ID = "deviceID";
  public static final String DEVICE_META_DATA = "deviceMetadata";
  public static final String RAW_DATA = "rawData";
  public static final String DATA_PROCESSING_METHOD = "dataProcessingMethod";
  public static final String BIZ_RULES = "bizRules";

  // Sensor Report info
  public static final String EXCEPTION = "exception";
  public static final String MICROORGANISM = "microorganism";
  public static final String CHEMICAL_SUBSTANCE = "chemicalSubstance";
  public static final String VALUE = "value";
  public static final String COMPONENT = "component";
  public static final String STRING_VALUE = "stringValue";
  public static final String BOOLEAN_VALUE = "booleanValue";
  public static final String HEX_BINARY_VALUE = "hexBinaryValue";
  public static final String URI_VALUE = "uriValue";
  public static final String MIN_VALUE = "minValue";
  public static final String MAX_VALUE = "maxValue";
  public static final String MEAN_VALUE = "meanValue";
  public static final String S_DEV = "sDev";
  public static final String PERC_RANK = "percRank";
  public static final String PERC_VALUE = "percValue";
  public static final String COORDINATE_REFERENCE_SYSTEM = "coordinateReferenceSystem";

  // Error declaration info
  public static final String ERROR_DECLARATION = "errorDeclaration";
  public static final String DECLARATION_TIME = "declarationTime";
  public static final String REASON = "reason";
  public static final String CORRECTIVE_EVENT_IDS = "correctiveEventIDs";
  public static final String CORRECTIVE_EVENT_ID = "correctiveEventID";

  // Other basic info
  public static final String EVENT_ID = "eventID";
  public static final String CERTIFICATION_INFO = "certificationInfo";
  public static final String ACTION = "action";
  public static final String TRANSFORMATION_ID = "transformationID";

  // Other GS1 specific info
  public static final String CONTEXT = "@context";

  public static final String CBV_MDA = "cbvmda";
  public static final String CBV_MDA_URL = "urn:epcglobal:cbv:mda";
  public static final String EPCIS_DOCUMENT = "EPCISDocument";
  public static final String EPCIS_DOCUMENT_WITH_NAMESPACE = "epcis:EPCISDocument";
  public static final String EPCIS_BODY = "EPCISBody";
  public static final String EPCIS_EVENTS_LIST = "EventList";
  public static final String PATH_DELIMITER = "/";
  public static final String GS1_FORMATTED_VALUE = "gs1:";
  public static final String INSTANCE_IDENTIFIER_URN_PREFIX = "urn:epc:id:";
  public static final String CLASS_IDENTIFIER_URN_PREFIX_WITH_CLASS = "urn:epc:class:";
  public static final String CLASS_IDENTIFIER_URN_PREFIX_WITH_IDPAT = "urn:epc:idpat:";
  public static final String PARTY_GLN_IDENTIFIER_URN_PREFIX = "urn:epc:id:pgln:";
  public static final String SERIALIZED_GLN_IDENTIFIER_URN_PREFIX = "urn:epc:id:sgln:";
  public static final String BIZ_STEP_EPC_URN_PREFIX = "urn:epcglobal:cbv:bizstep:";
  public static final String DISPOSITION_EPC_URN_PREFIX = "urn:epcglobal:cbv:disp:";
  public static final String BIZ_TRANSACTION_EPC_URN_PREFIX = "urn:epcglobal:cbv:btt:";
  public static final String SOURCE_DESTINATION_EPC_URN_PREFIX = "urn:epcglobal:cbv:sdt:";
  public static final String ERROR_REASON_EPC_URN_PREFIX = "urn:epcglobal:cbv:er:";
}
