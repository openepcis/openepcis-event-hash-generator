package io.openecpis.epc.eventhash.azure;

import io.openepcis.epc.eventhash.EventHashGenerator;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Produces;

public class EventHashGeneratorProducer {

  @Produces
  @RequestScoped
  public EventHashGenerator eventHashGenerator() {
    return new EventHashGenerator();
  }
}
