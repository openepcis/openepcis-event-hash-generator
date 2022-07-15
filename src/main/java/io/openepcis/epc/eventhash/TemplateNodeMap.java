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
              || ConstantEventHashInfo.SENSOR_REPORT.equals(parent.getName()))) {
        path.push(parent.getName());
      } else if (parent.getParent() != null
          && parent.getParent().getName() != null
          && (!ConstantEventHashInfo.LIST_OF_OBJECTS.containsValue(parent.getParent().getName())
              || ConstantEventHashInfo.SENSOR_REPORT.equals(parent.getParent().getName()))) {
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
      // Special handling for SensorElementList & SensorElement tag, so it can be added once if the
      // SensorElements have the UserExtensions present within them.
      if (ConstantEventHashInfo.USER_EXTENSION_WRAPPER.stream().anyMatch(node.getName()::equals)) {
        final AtomicReference<Integer> found = new AtomicReference<>(0);
        node.getChildren()
            .forEach(
                sensorChild -> {
                  if (addExtensionWrapperTag(sensorChild)) {
                    found.set(found.get() + 1);
                  }
                });
        return found.get() > 0;
      } else {
        // For all other fields except sensorElementList & SensorElement
        final AtomicReference<Integer> found = new AtomicReference<>(0);
        node.getChildren()
            .forEach(
                element -> {
                  if (element.getName() != null && !isEpcisField(element)) {
                    found.set(found.get() + 1);
                  }
                });
        return found.get() > 0;
      }
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
    put("type", new LinkedHashMap<>());

    put("eventTime", new LinkedHashMap<>());

    put("eventTimeZoneOffset", new LinkedHashMap<>());

    final LinkedHashMap<String, Object> errorCorrectiveId = new LinkedHashMap<>();
    errorCorrectiveId.put("correctiveEventID", new LinkedHashMap<>());

    final LinkedHashMap<String, Object> errorDeclaration = new LinkedHashMap<>();
    errorDeclaration.put("declarationTime", new LinkedHashMap<>());
    errorDeclaration.put("reason", new LinkedHashMap<>());
    errorDeclaration.put("correctiveEventIDs", errorCorrectiveId);

    put("errorDeclaration", errorDeclaration);

    final LinkedHashMap<String, Object> instanceIdentifier = new LinkedHashMap<>();
    instanceIdentifier.put("epc", new LinkedHashMap<>());

    put("epcList", instanceIdentifier);

    put("parentID", new LinkedHashMap<>());

    put("inputEPCList", instanceIdentifier);

    put("childEPCs", instanceIdentifier);

    final LinkedHashMap<String, Object> classIdentifier = new LinkedHashMap<>();
    classIdentifier.put("epcClass", new LinkedHashMap<>());
    classIdentifier.put("quantity", new LinkedHashMap<>());
    classIdentifier.put("uom", new LinkedHashMap<>());

    put("quantityList", classIdentifier);

    put("childQuantityList", classIdentifier);

    put("inputQuantityList", classIdentifier);

    put("outputEPCList", instanceIdentifier);

    put("outputQuantityList", classIdentifier);

    put("action", new LinkedHashMap<>());

    put("transformationID", new LinkedHashMap<>());

    put("bizStep", new LinkedHashMap<>());

    put("disposition", new LinkedHashMap<>());

    final LinkedHashMap<String, Object> persistentDisposition = new LinkedHashMap<>();
    persistentDisposition.put("set", new LinkedHashMap<>());
    persistentDisposition.put("unset", new LinkedHashMap<>());

    put("persistentDisposition", persistentDisposition);

    final LinkedHashMap<String, Object> readPoint = new LinkedHashMap<>();
    readPoint.put("id", new LinkedHashMap<>());

    put("readPoint", readPoint);

    final LinkedHashMap<String, Object> bizLocation = new LinkedHashMap<>();
    bizLocation.put("id", new LinkedHashMap<>());

    put("bizLocation", bizLocation);

    final LinkedHashMap<String, Object> bizTransactionList = new LinkedHashMap<>();
    bizTransactionList.put("bizTransaction", new LinkedHashMap<>());
    bizTransactionList.put("type", new LinkedHashMap<>());

    put("bizTransactionList", bizTransactionList);

    final LinkedHashMap<String, Object> sourceList = new LinkedHashMap<>();
    sourceList.put("source", new LinkedHashMap<>());
    sourceList.put("type", new LinkedHashMap<>());

    put("sourceList", sourceList);

    final LinkedHashMap<String, Object> destinationList = new LinkedHashMap<>();
    destinationList.put("destination", new LinkedHashMap<>());
    destinationList.put("type", new LinkedHashMap<>());

    put("destinationList", destinationList);

    final LinkedHashMap<String, Object> sensorMetadata = new LinkedHashMap<>();
    sensorMetadata.put("time", new LinkedHashMap<>());
    sensorMetadata.put("startTime", new LinkedHashMap<>());
    sensorMetadata.put("endTime", new LinkedHashMap<>());
    sensorMetadata.put("deviceID", new LinkedHashMap<>());
    sensorMetadata.put("deviceMetadata", new LinkedHashMap<>());
    sensorMetadata.put("rawData", new LinkedHashMap<>());
    sensorMetadata.put("dataProcessingMethod", new LinkedHashMap<>());
    sensorMetadata.put("bizRules", new LinkedHashMap<>());

    final LinkedHashMap<String, Object> sensorReport = new LinkedHashMap<>();
    sensorReport.put("type", new LinkedHashMap<>());
    sensorReport.put("deviceID", new LinkedHashMap<>());
    sensorReport.put("deviceMetadata", new LinkedHashMap<>());
    sensorReport.put("rawData", new LinkedHashMap<>());
    sensorReport.put("dataProcessingMethod", new LinkedHashMap<>());
    sensorReport.put("time", new LinkedHashMap<>());
    sensorReport.put("microorganism", new LinkedHashMap<>());
    sensorReport.put("chemicalSubstance", new LinkedHashMap<>());
    sensorReport.put("value", new LinkedHashMap<>());
    sensorReport.put("component", new LinkedHashMap<>());
    sensorReport.put("stringValue", new LinkedHashMap<>());
    sensorReport.put("booleanValue", new LinkedHashMap<>());
    sensorReport.put("hexBinaryValue", new LinkedHashMap<>());
    sensorReport.put("uriValue", new LinkedHashMap<>());
    sensorReport.put("minValue", new LinkedHashMap<>());
    sensorReport.put("maxValue", new LinkedHashMap<>());
    sensorReport.put("meanValue", new LinkedHashMap<>());
    sensorReport.put("sDev", new LinkedHashMap<>());
    sensorReport.put("percRank", new LinkedHashMap<>());
    sensorReport.put("percValue", new LinkedHashMap<>());
    sensorReport.put("uom", new LinkedHashMap<>());

    LinkedHashMap<String, Object> sensorElementList = new LinkedHashMap<>();
    sensorElementList.put("sensorMetadata", sensorMetadata);
    sensorElementList.put("sensorReport", sensorReport);

    put("sensorElementList", sensorElementList);

    put("ilmd", new LinkedHashMap<>());
  }
}
