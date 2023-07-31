package openepcis.epc.eventhash.generator.servlet.test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import openepcis.epc.eventhash.generator.servlet.EventHashGeneratorServlets;
import openepcis.epc.eventhash.generator.test.AbstractEventHashGeneratorTest;

import java.net.URL;

@QuarkusTest
public class EventHashGeneratorServletTest extends AbstractEventHashGeneratorTest {

    @TestHTTPEndpoint(EventHashGeneratorServlets.EPCISDocument.class)
    @TestHTTPResource
    URL epcisDocumentUrl;

    @TestHTTPEndpoint(EventHashGeneratorServlets.EPCISEvents.class)
    @TestHTTPResource
    URL epcisEventsUrl;

    @Override
    protected String documentApi() {
        return epcisDocumentUrl.toString();
    }

    @Override
    protected String eventsApi() {
        return epcisEventsUrl.toString();
    }
}
