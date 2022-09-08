package openepcis.epc.eventhash.generator;

import com.fasterxml.jackson.core.JsonFactory;
import javax.enterprise.inject.Produces;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;

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

  @Produces
  public JsonFactory createJsonFactory() {
    return new JsonFactory();
  }
}
