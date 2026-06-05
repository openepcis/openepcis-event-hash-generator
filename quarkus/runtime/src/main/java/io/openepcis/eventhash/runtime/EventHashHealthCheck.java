/*
 * Copyright 2022-2026 benelog GmbH & Co. KG
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
import io.openepcis.eventhash.HashIdGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class EventHashHealthCheck implements HealthCheck {

    // Known SHA-256 of the empty string, with CBV 2.0 URI envelope. A mismatch means hashing is broken
    private static final String EXPECTED_HASH = "ni:///sha-256;e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855?ver=CBV2.0";

    @Override
    public HealthCheckResponse call() {
        final HealthCheckResponseBuilder builder = HealthCheckResponse.named("OpenEPCIS Event Hash Generator health check").up();

        try {
            // Compute a known hash via the production code
            final String actual = HashIdGenerator.generateHashId("", "sha-256", CBVVersion.VERSION_2_0_0);

            // Match: report UP with a PASS marker
            if (EXPECTED_HASH.equals(actual)) {
                return builder.up()
                        .withData("smokeTest", "PASS")
                        .build();
            }

            // Mismatch: report DOWN and surface both values so operators can diagnose without code dive.
            return builder.down()
                    .withData("smokeTest", "FAIL")
                    .withData("expected", EXPECTED_HASH)
                    .withData("actual", actual)
                    .build();
        } catch (Exception e) {
            // Any Exception: DOWN with the error message attached.
            return builder.down()
                    .withData("smokeTest", "ERROR")
                    .withData("error", e.getClass().getSimpleName() + ": " + e.getMessage())
                    .build();
        }
    }
}
