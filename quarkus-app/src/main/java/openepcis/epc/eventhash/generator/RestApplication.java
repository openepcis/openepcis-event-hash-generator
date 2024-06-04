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
package openepcis.epc.eventhash.generator;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openepcis.epc.eventhash.EventHashGenerator;
import io.openepcis.resources.oas.EPCISExampleOASFilter;
import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@OpenAPIDefinition(
    info =
        @Info(
            title = "OpenEPCIS Event Hash Generator REST API",
            description = "Generate Hash-Ids for EPCIS documents in XML/JSON format.",
            version = "0.9.1",
            license =
                @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0")))
@ApplicationPath("/")
public class RestApplication extends Application {

  private EPCISExampleOASFilter filter;

  @Produces
  @RequestScoped
  public EventHashGenerator createEventHashGenerator() {
    return new EventHashGenerator();
  }

  @Produces
  public JsonFactory createJsonFactory() {
    final ObjectMapper objectMapper =
        new ObjectMapper(); // .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    return new JsonFactory().setCodec(objectMapper);
  }

  @Route(methods = Route.HttpMethod.GET, path = "/")
  @Operation(hidden = true)
  void hello(RoutingContext rc) {
    rc.redirect("/q/swagger-ui/index.html");
  }
}
