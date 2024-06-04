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
package openepcis.epc.eventhash.generator.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openepcis.epc.eventhash.EventHashGenerator;
import io.openepcis.model.rest.ProblemResponseBody;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

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
