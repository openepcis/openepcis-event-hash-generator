package openepcis.epc.eventhash.generator.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openepcis.epc.eventhash.EventHashGenerator;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;

public class CDITestProvider {

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

}
