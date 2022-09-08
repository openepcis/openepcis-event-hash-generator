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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Class which implements Comparator to compare 2 different nodes for sorting. Based on the results
 * from Comparator the information of the nodes are placed accordingly in pre-hash string.
 */
public class HashNodeComparator implements Comparator<ContextNode> {
  private final List<String> sortMap;

  HashNodeComparator(final ContextNode jsonNode) {
    this.sortMap = TemplateNodeMap.findSortList(jsonNode);
  }

  @Override
  public int compare(ContextNode o1, ContextNode o2) {
    if (o1.getName() != null && o2.getName() != null) {

      // Initialize the value for index as max value to indicate no matches found. If match found
      // then assign the values accordingly.
      int o1Index = Integer.MAX_VALUE;
      int o2Index = Integer.MAX_VALUE;

      // Check if the inner value ordering is done or parent. Condition has been added to remove the
      // confusion around fields with same name such as type (ObjectEvent, AggregationEvent) and
      // type (bizTransaction, Source, Destination)
      if (o1.getParent() != null && o2.getParent() != null) {
        o1Index = sortMap.indexOf(o1.getName());
        o2Index = sortMap.indexOf(o2.getName());
      }

      // Sort the outer event fields such as action, bizStep, eventTime, etc.
      final int result = Integer.compare(o1Index, o2Index);

      // For the User Extensions field when both elements are not part of the epcis standard fields
      // then sort them based on the name.
      if (result == 0 && o1Index == -1 && o2Index == -1) {
        if (o1.getName() != null && o2.getName() != null) {
          return o1.getName().compareTo(o2.getName());
        }
      } else if (result == 0) {
        // If the outer event fields are equal then sort the inner elements of the fields' ex:
        // errorDeclaration. If the inner fields have the value then compare them and return.
        if (o1.getValue() != null && o2.getValue() != null) {
          return o1.getValue().compareTo(o2.getValue());
        } else if (o1.getChildren() != null && o2.getChildren() != null) {
          return findChildren(o1.getChildren()).compareTo(findChildren(o2.getChildren()));
        }
      } else if (o1Index == -1) {
        return 1;
      } else if (o2Index == -1) {
        return -1;
      } else {
        return result;
      }
    }
    return 0;
  }

  // For nested hashnode values loop over its children and get values.
  private String findChildren(ArrayList<ContextNode> children) {
    final StringBuilder childrenString = new StringBuilder();

    if (children != null) {
      children.forEach(
          child -> {
            if (child != null && child.getValue() == null) {
              childrenString.append(findChildren(child.getChildren()));
            } else if (child != null && child.getValue() != null) {
              childrenString.append(child.getValue());
            }
          });
    }

    return childrenString.toString();
  }
}
