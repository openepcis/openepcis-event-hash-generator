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
package io.openepcis.epc.eventhash.main;

import io.openepcis.epc.eventhash.EventHashGenerator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class JsonDocumentEventHashGenerator {

  // Method to generate Hash IDs for the EPCIS JSON/JSON-LD document
  public static void main(String[] args)
      throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException,
          IOException {
    final EventHashGenerator jsonHashGenerator = new EventHashGenerator();
    final InputStream jsonStream =
        new ByteArrayInputStream(args[0].getBytes(StandardCharsets.UTF_8));

    System.out.println("Event Hash ids for JSON/JSON-LD events in EPCIS document:");
    jsonHashGenerator
        .jsonDocumentHashIdGenerator(jsonStream, args[1])
        .subscribe()
        .with(
            System.out::println,
            failure -> System.out.println("Failed with error : " + failure),
            () ->
                System.out.println(
                    "Event hash generator for JSON/JSON-LD EPCIS document finished."));
  }
}
