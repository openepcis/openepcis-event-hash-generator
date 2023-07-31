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
