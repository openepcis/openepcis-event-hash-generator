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
package io.openepcis.eventhash.generator.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openepcis.eventhash.EventHashGenerator;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;

public class CDITestProvider {
  final ObjectMapper objectMapper =
      new ObjectMapper(); // .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

  @Produces
  @RequestScoped
  public EventHashGenerator createEventHashGenerator() {
    return new EventHashGenerator();
  }

  @Produces
  public JsonFactory createJsonFactory() {
    return new JsonFactory().setCodec(objectMapper);
  }

  @Produces
  ObjectMapper objectMapper() {
    return objectMapper;
  }
}
