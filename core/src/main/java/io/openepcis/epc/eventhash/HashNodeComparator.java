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
  private ContextNode contextNode = new ContextNode();

  private Boolean standardFieldSort;

  HashNodeComparator(final ContextNode jsonNode, final Boolean standardFieldSort) {
    this.sortMap = TemplateNodeMap.findSortList(jsonNode);
    this.standardFieldSort = standardFieldSort;
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
        // Check if the element is of user extension
        if ((!TemplateNodeMap.isEpcisField(o1) && !TemplateNodeMap.isEpcisField(o2))
            || (contextNode.isIlmdPath(o1) && contextNode.isIlmdPath(o2))) {
          // For user extensions consider the namespace and then sort
          return sortUserExtensions(o1, o2);
        } else if (o1.getName() != null
            && o2.getName() != null
            && o1.getName().equals(o2.getName())
            && o1.getChildren() != null
            && o2.getChildren() != null) {
          return findChildren(o1.getChildren()).compareTo(findChildren(o2.getChildren()));
        } else if (o1.getName() != null && o2.getName() != null) {
          // For dedicated epcis fields sort based on the name
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
    } else if (o1.getChildren() != null && o2.getChildren() != null) {
      return findChildren(o1.getChildren()).compareTo(findChildren(o2.getChildren()));
    }
    return 0;
  }

  private int sortUserExtensions(final ContextNode o1, final ContextNode o2) {
    // Ensure only user extensions are sorted
    if (o1.getName().contains(":") && o2.getName().contains(":")) {
      final String o1Namespace =
          o1.getName() != null
              ? o1.getNamespaces().get(o1.getName().substring(0, o1.getName().indexOf(":")))
              : null;
      final String o2Namespace =
          o2.getName() != null
              ? o2.getNamespaces().get(o2.getName().substring(0, o2.getName().indexOf(":")))
              : null;
      final String o1String =
          o1Namespace != null
              ? o1Namespace
                  + o1.getName().substring(o1.getName().indexOf(":") + 1)
                  + "="
                  + o1.getValue()
              : o1.getName();
      final String o2String =
          o2Namespace != null
              ? o2Namespace
                  + o2.getName().substring(o2.getName().indexOf(":") + 1)
                  + "="
                  + o2.getValue()
              : o2.getName();
      return o1String.compareTo(o2String);
    }
    return o1.getName().compareTo(o2.getName());
  }

  // For nested hashnode values loop over its children and get values.
  private String findChildren(ArrayList<ContextNode> children) {
    final StringBuilder childrenString = new StringBuilder();
    final StringBuilder extensionString = new StringBuilder();

    if (children != null) {
      children.forEach(
          child -> {
            // Sort only based on dedicated epcis field and ignore the extension values present in
            // children
            if (child != null
                && child.getValue() == null
                && TemplateNodeMap.isEpcisField(child)
                && standardFieldSort) {
              childrenString.append(findChildren(child.getChildren()));
            } else if (child != null
                && child.getValue() != null
                && TemplateNodeMap.isEpcisField(child)
                && standardFieldSort) {
              // For the identifiers convert them to URI format before sort
              childrenString.append(
                  contextNode.epcisFieldFormatter(child.getName(), child.getValue(), child));
            } else if (child != null
                && child.getValue() == null
                && !TemplateNodeMap.isEpcisField(child)
                && !standardFieldSort) {
              // For extension append to extension string only the extension elements
              extensionString.append(findChildren(child.getChildren()));
            } else if (child != null
                && child.getValue() != null
                && !TemplateNodeMap.isEpcisField(child)
                && !standardFieldSort) {
              // For extension append to extension string only the extension values formatted
              extensionString.append(
                  contextNode.userExtensionsFormatter(
                      child.getName(), child.getValue(), child.getNamespaces()));
            }
          });
    }

    return standardFieldSort ? childrenString.toString() : extensionString.toString();
  }
}
