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
package io.openepcis.eventhash.deployment;

import io.openepcis.eventhash.runtime.EventHashGeneratorProducer;
import io.openepcis.eventhash.runtime.EventHashHealthCheck;
import io.openepcis.quarkus.deployment.model.OpenEPCISBuildTimeConfig;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

public class OpenEPCISEventHashProcessor {

  private static final String FEATURE = "openepcis-event-hash";


  @BuildStep
  FeatureBuildItem feature() {
    return new FeatureBuildItem(FEATURE);
  }

  @BuildStep()
  AdditionalBeanBuildItem buildOpenEPCISJAXBContext() {
    return AdditionalBeanBuildItem.unremovableOf(EventHashGeneratorProducer.class);
  }

  @BuildStep
  HealthBuildItem addHealthCheck(OpenEPCISBuildTimeConfig buildTimeConfig) {
    return new HealthBuildItem(EventHashHealthCheck.class.getName(),
            buildTimeConfig.healthEnabled);
  }

}
