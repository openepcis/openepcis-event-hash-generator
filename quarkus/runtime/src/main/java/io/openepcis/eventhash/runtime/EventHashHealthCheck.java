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

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class EventHashHealthCheck implements HealthCheck {

    private final EventHashGeneratorProducer eventHashGeneratorProducer;

    public EventHashHealthCheck(final EventHashGeneratorProducer eventHashGeneratorProducer) {
        this.eventHashGeneratorProducer = eventHashGeneratorProducer;
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("OpenEPCIS Event Hash Generator health check").up();
        builder.up().withData("eventHashGeneratorProducer", eventHashGeneratorProducer.getClass().getName());
        return builder.build();
    }
}
