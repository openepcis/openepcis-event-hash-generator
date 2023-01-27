package io.openepcis.epc.eventhash.exception;

public class EventHashException extends RuntimeException {
  public EventHashException(String msg, Throwable parent) {
    super(msg, parent);
  }

  public EventHashException(String msg) {
    super(msg);
  }

  public EventHashException(Throwable parent) {
    super(parent);
  }
}
