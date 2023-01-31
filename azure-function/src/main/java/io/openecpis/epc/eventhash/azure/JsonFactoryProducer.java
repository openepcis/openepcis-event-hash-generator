package io.openecpis.epc.eventhash.azure;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.enterprise.inject.Produces;

public class JsonFactoryProducer {

  @Produces
  public JsonFactory createJsonFactory() {
    final ObjectMapper objectMapper =
        new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    return new JsonFactory().setCodec(objectMapper);
  }
}
