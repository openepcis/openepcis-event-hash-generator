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
package io.openepcis.eventhash.runtime;

import io.openepcis.constants.CBVVersion;
import io.openepcis.eventhash.EventHashGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class EventHashGeneratorProducer {

    // Configurable default CBV version. Can be overridden at startup with -Dopenepcis.event-hash.cbv-version=2.1.0
    @ConfigProperty(name = "openepcis.event-hash.cbv-version", defaultValue = "2.0.0")
    String cbvVersion;

    @Produces
    @RequestScoped
    public EventHashGenerator createEventHashGenerator() {
        // Parse to get the correct enum value, defaulting to 2.0 if parsing fails
        final CBVVersion version = CBVVersion.of(cbvVersion);
        return new EventHashGenerator(version);
    }
}
