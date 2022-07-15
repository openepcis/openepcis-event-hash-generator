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
package io.openepcis.epc.eventhash.publisher;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ObjectNodeUtil {

  protected static final List<String> REQUIRED_DOCUMENT_FIELDS =
      Arrays.asList("@context", "type", "schemaVersion", "creationDate");

  public static boolean isValidEPCISDocumentNode(final ObjectNode header) {
    for (String field : REQUIRED_DOCUMENT_FIELDS) {
      if (!header.has(field)) {
        return false;
      }
    }
    return true;
  }
}
