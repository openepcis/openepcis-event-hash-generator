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
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class TemplateNodeMap extends LinkedHashMap<String, Object> {

  private static final TemplateNodeMap _INSTANCE = new TemplateNodeMap();

  public static TemplateNodeMap getInstance() {
    return _INSTANCE;
  }

  public static List<String> findSortList(final ContextNode node) {
    // If the incoming node has the values then populate them in stack
    final Deque<String> path = new ArrayDeque<>();
    if (node.getName() != null) {
      path.push(node.getName());
    }
    ContextNode parent = node.getParent();

    // If the incoming node has complex structure and has the children elements then add children
    // elements into the stack
    while (parent != null && parent.getName() != null) {
      path.push(parent.getName());
      parent = parent.getParent();
    }

    // If the path is empty then return the outer elements list like type,eventTime,sourceList, etc
    if (path.isEmpty()) {
      return new ArrayList<>(getInstance().keySet());
    }

    LinkedHashMap<String, Object> entry = null;
    String s = path.pop();

    while (s != null) {
      if (entry == null) {
        entry = (LinkedHashMap<String, Object>) getInstance().get(s);
      } else if (entry.get(s) != null) {
        entry = (LinkedHashMap<String, Object>) entry.get(s);
      }
      if (!path.isEmpty()) {
        s = path.pop();
      } else {
        s = null;
      }
    }

    if (entry != null && !entry.isEmpty()) {
      return new ArrayList<>(entry.keySet());
    }

    return Collections.emptyList();
  }

  // Method to identify if the field is part of EPCIS standard field during creation of pre-hash
  // string for JSON/JSON-LD events.
  public static boolean isEpcisField(final ContextNode node) {

    // Call the method to get the path of the every Node
    final Deque<String> path = pathFinder(node);

    if (Boolean.TRUE.equals(isIlmdField(path))) {
      return true;
    } else {
      LinkedHashMap<String, Object> current = getInstance();
      int found = 0;
      Iterator<String> iter = path.iterator();

      while (iter.hasNext() && current != null) {
        final String key = iter.next();
        if (current.containsKey(key)) {
          found++;
          current = (LinkedHashMap<String, Object>) current.get(key);
        } else {
          current = null;
        }
      }
      return (found == path.size() && !path.isEmpty())
          || (node.getName() != null
              && ConstantEventHashInfo.LIST_OF_OBJECTS.containsValue(node.getName()));
    }
  }

  // Private method to get the path of the element which will be used later to find if the element
  // is part of EPCIS standard fields or not
  private static Deque<String> pathFinder(final ContextNode node) {
    // If the incoming node has the values then populate them in stack
    final Deque<String> path = new ArrayDeque<>();
    path.push(node.getName() != null ? node.getName() : "");
    ContextNode parent = node.getParent();

    // If the incoming node has complex structure and has the children elements then add children
    // elements into the stack.
    // Add only those elements to Deque which are part of the EPCIS standard. Do not add the
    // keywords added during the reading of event for pre-hash string purpose such as SensorElement,
    // QuantityElement
    while (parent != null) {
      if (parent.getName() != null
          && !path.contains(parent.getName())
          && (!ConstantEventHashInfo.LIST_OF_OBJECTS.containsValue(parent.getName())
              || EPCIS.SENSOR_REPORT.equals(parent.getName()))) {
        path.push(parent.getName());
      } else if (parent.getParent() != null
          && parent.getParent().getName() != null
          && (!ConstantEventHashInfo.LIST_OF_OBJECTS.containsValue(parent.getParent().getName())
              || EPCIS.SENSOR_REPORT.equals(parent.getParent().getName()))) {
        path.push(parent.getParent().getName());
      }
      parent = parent.getParent();
    }
    return path;
  }

  // Method to check if the field contains the user extensions as its children elements. If so then
  // EPCIS field tag needs to be added to differentiate the different types of User-Extensions.
  public static boolean addExtensionWrapperTag(final ContextNode node) {
    if (node.getName() != null && isEpcisField(node)) {
      final AtomicReference<Integer> found = new AtomicReference<>(0);
      // Special handling for SensorElementList & SensorElement tag, so it can be added once if the
      // SensorElements have the UserExtensions present within them.
      if (ConstantEventHashInfo.USER_EXTENSION_WRAPPER.stream().anyMatch(node.getName()::equals)) {
        node.getChildren()
            .forEach(
                sensorChild -> {
                  if (addExtensionWrapperTag(sensorChild)) {
                    found.set(found.get() + 1);
                  }
                });
      } else {
        // For all other fields except sensorElementList & SensorElement
        node.getChildren()
            .forEach(
                element -> {
                  if (!element.getChildren().isEmpty()) {
                    if (addExtensionWrapperTag(element)) {
                      found.set(found.get() + 1);
                    }
                  } else if (element.getChildren().isEmpty()
                      && element.getName() != null
                      && !isEpcisField(element)) {
                    found.set(found.get() + 1);
                  }
                });
      }
      return found.get() > 0;
    }
    return false;
  }

  // Special handling for the ILMD fields as it contains User Extensions like elements but should
  // appear before User-Extensions as well known fields of EPCIS standard.
  // Return true if the elements are part of ILMD childrens, so it would appear always on the top
  // before other User-Extensions
  private static Boolean isIlmdField(final Deque<String> key) {
    return key.contains("ilmd");
  }

  private TemplateNodeMap() {
    put(EPCIS.TYPE, new LinkedHashMap<>());

    put(EPCIS.EVENT_TIME, new LinkedHashMap<>());

    put(EPCIS.EVENT_TIME_ZONE_OFFSET, new LinkedHashMap<>());

    put(EPCIS.CERTIFICATION_INFO, new LinkedHashMap<>());

    final LinkedHashMap<String, Object> instanceIdentifier = new LinkedHashMap<>();
    instanceIdentifier.put(EPCIS.EPC, new LinkedHashMap<>());

    put(EPCIS.EPC_LIST, instanceIdentifier);

    put(EPCIS.PARENT_ID, new LinkedHashMap<>());

    put(EPCIS.INPUT_EPC_LIST, instanceIdentifier);

    put(EPCIS.CHILD_EPCS, instanceIdentifier);

    final LinkedHashMap<String, Object> classIdentifier = new LinkedHashMap<>();
    classIdentifier.put(EPCIS.EPC_CLASS, new LinkedHashMap<>());
    classIdentifier.put(EPCIS.QUANTITY, new LinkedHashMap<>());
    classIdentifier.put(EPCIS.UOM, new LinkedHashMap<>());

    put(EPCIS.QUANTITY_LIST, classIdentifier);

    put(EPCIS.CHILD_QUANTITY_LIST, classIdentifier);

    put(EPCIS.INPUT_QUANTITY_LIST, classIdentifier);

    put(EPCIS.OUTPUT_EPC_LIST, instanceIdentifier);

    put(EPCIS.OUTPUT_QUANTITY_LIST, classIdentifier);

    put(EPCIS.ACTION, new LinkedHashMap<>());

    put(EPCIS.TRANSFORMATION_ID, new LinkedHashMap<>());

    put(EPCIS.BIZ_STEP, new LinkedHashMap<>());

    put(EPCIS.DISPOSITION, new LinkedHashMap<>());

    final LinkedHashMap<String, Object> persistentDisposition = new LinkedHashMap<>();
    persistentDisposition.put(EPCIS.SET, new LinkedHashMap<>());
    persistentDisposition.put(EPCIS.UNSET, new LinkedHashMap<>());

    put(EPCIS.PERSISTENT_DISPOSITION, persistentDisposition);

    final LinkedHashMap<String, Object> readPoint = new LinkedHashMap<>();
    readPoint.put(EPCIS.ID, new LinkedHashMap<>());

    put(EPCIS.READ_POINT, readPoint);

    final LinkedHashMap<String, Object> bizLocation = new LinkedHashMap<>();
    bizLocation.put(EPCIS.ID, new LinkedHashMap<>());

    put(EPCIS.BIZ_LOCATION, bizLocation);

    final LinkedHashMap<String, Object> bizTransactionList = new LinkedHashMap<>();
    bizTransactionList.put(EPCIS.TYPE, new LinkedHashMap<>());
    bizTransactionList.put(EPCIS.BIZ_TRANSACTION, new LinkedHashMap<>());

    put(EPCIS.BIZ_TRANSACTION_LIST, bizTransactionList);

    final LinkedHashMap<String, Object> sourceList = new LinkedHashMap<>();
    sourceList.put(EPCIS.TYPE, new LinkedHashMap<>());
    sourceList.put(EPCIS.SOURCE, new LinkedHashMap<>());

    put(EPCIS.SOURCE_LIST, sourceList);

    final LinkedHashMap<String, Object> destinationList = new LinkedHashMap<>();
    destinationList.put(EPCIS.TYPE, new LinkedHashMap<>());
    destinationList.put(EPCIS.DESTINATION, new LinkedHashMap<>());

    put(EPCIS.DESTINATION_LIST, destinationList);

    final LinkedHashMap<String, Object> sensorMetadata = new LinkedHashMap<>();
    sensorMetadata.put(EPCIS.TIME, new LinkedHashMap<>());
    sensorMetadata.put(EPCIS.START_TIME, new LinkedHashMap<>());
    sensorMetadata.put(EPCIS.END_TIME, new LinkedHashMap<>());
    sensorMetadata.put(EPCIS.DEVICE_ID, new LinkedHashMap<>());
    sensorMetadata.put(EPCIS.DEVICE_META_DATA, new LinkedHashMap<>());
    sensorMetadata.put(EPCIS.RAW_DATA, new LinkedHashMap<>());
    sensorMetadata.put(EPCIS.DATA_PROCESSING_METHOD, new LinkedHashMap<>());
    sensorMetadata.put(EPCIS.BIZ_RULES, new LinkedHashMap<>());

    final LinkedHashMap<String, Object> sensorReport = new LinkedHashMap<>();
    sensorReport.put(EPCIS.TYPE, new LinkedHashMap<>());
    sensorReport.put(EPCIS.EXCEPTION, new LinkedHashMap<>());
    sensorReport.put(EPCIS.DEVICE_ID, new LinkedHashMap<>());
    sensorReport.put(EPCIS.DEVICE_META_DATA, new LinkedHashMap<>());
    sensorReport.put(EPCIS.RAW_DATA, new LinkedHashMap<>());
    sensorReport.put(EPCIS.DATA_PROCESSING_METHOD, new LinkedHashMap<>());
    sensorReport.put(EPCIS.TIME, new LinkedHashMap<>());

    sensorReport.put(EPCIS.MICROORGANISM, new LinkedHashMap<>());
    sensorReport.put(EPCIS.CHEMICAL_SUBSTANCE, new LinkedHashMap<>());
    sensorReport.put(EPCIS.VALUE, new LinkedHashMap<>());
    sensorReport.put(EPCIS.COMPONENT, new LinkedHashMap<>());
    sensorReport.put(EPCIS.STRING_VALUE, new LinkedHashMap<>());
    sensorReport.put(EPCIS.BOOLEAN_VALUE, new LinkedHashMap<>());
    sensorReport.put(EPCIS.HEX_BINARY_VALUE, new LinkedHashMap<>());
    sensorReport.put(EPCIS.URI_VALUE, new LinkedHashMap<>());
    sensorReport.put(EPCIS.MIN_VALUE, new LinkedHashMap<>());
    sensorReport.put(EPCIS.MAX_VALUE, new LinkedHashMap<>());
    sensorReport.put(EPCIS.MEAN_VALUE, new LinkedHashMap<>());
    sensorReport.put(EPCIS.S_DEV, new LinkedHashMap<>());
    sensorReport.put(EPCIS.PERC_RANK, new LinkedHashMap<>());
    sensorReport.put(EPCIS.PERC_VALUE, new LinkedHashMap<>());
    sensorReport.put(EPCIS.UOM, new LinkedHashMap<>());
    sensorReport.put(EPCIS.COORDINATE_REFERENCE_SYSTEM, new LinkedHashMap<>());

    LinkedHashMap<String, Object> sensorElementList = new LinkedHashMap<>();
    sensorElementList.put(EPCIS.SENSOR_METADATA, sensorMetadata);
    sensorElementList.put(EPCIS.SENSOR_REPORT, sensorReport);

    put(EPCIS.SENSOR_ELEMENT_LIST, sensorElementList);

    put(EPCIS.ILMD, new LinkedHashMap<>());
  }
}
