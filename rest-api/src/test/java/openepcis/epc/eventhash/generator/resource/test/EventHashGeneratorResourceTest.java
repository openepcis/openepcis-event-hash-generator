package openepcis.epc.eventhash.generator.resource.test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import openepcis.epc.eventhash.generator.resource.EventHashGeneratorResource;
import openepcis.epc.eventhash.generator.test.AbstractEventHashGeneratorTest;

import java.net.URL;

@QuarkusTest
public class EventHashGeneratorResourceTest extends AbstractEventHashGeneratorTest {

    @TestHTTPEndpoint(EventHashGeneratorResource.class)
    @TestHTTPResource
    URL url;

    @Override
    protected String documentApi() {
        return url + "/generate/event-hash/document";
    }

    @Override
    protected String eventsApi() {
        return url + "/generate/event-hash/events";
    }
}
