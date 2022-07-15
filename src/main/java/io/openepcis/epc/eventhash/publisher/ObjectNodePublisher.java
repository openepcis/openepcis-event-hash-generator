/*
 * Copyright 2022 benelog GmbH & Co. KG
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
package io.openepcis.epc.eventhash.publisher;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ObjectNodePublisher<T extends ObjectNode> implements Publisher<T> {
  private static final ObjectMapper mapper =
      new ObjectMapper().registerModule(new JavaTimeModule());
  private static final JsonFactory jsonFactory = new JsonFactory();

  private final ObjectNode header = mapper.createObjectNode();
  private final JsonParser jsonParser;

  private final AtomicBoolean headerSent = new AtomicBoolean(false);
  private final AtomicBoolean inEventList = new AtomicBoolean(false);
  private final AtomicBoolean ignoreEventList = new AtomicBoolean(false);
  private final AtomicLong nodeCount = new AtomicLong();
  private final AtomicReference<ObjectNodeSubscription> subscription = new AtomicReference<>();

  private JsonToken token;

  public ObjectNodePublisher(final InputStream in) throws IOException {
    this.jsonParser = jsonFactory.createParser(in);
    this.jsonParser.setCodec(mapper);
  }

  @Override
  public void subscribe(Subscriber<? super T> subscriber) {
    this.subscription.set(new ObjectNodeSubscription(subscriber));
    final Optional<Throwable> throwable = beginParsing();
    subscriber.onSubscribe(this.subscription.get());
    throwable.ifPresent(subscriber::onError);
  }

  /** start parsing input by processing all tokens up to eventList */
  private Optional<Throwable> beginParsing() {
    try {
      jsonParser.setCodec(mapper);
      token = jsonParser.nextToken();
      while (token != null && token != JsonToken.END_OBJECT) {
        final String fieldName = jsonParser.nextFieldName();
        token = jsonParser.nextToken();
        if ("eventList".equals(fieldName)) {
          if (token != JsonToken.START_ARRAY) {
            throw new IOException("invalid eventList structure, must be an array");
          }
          token = jsonParser.nextToken();
          inEventList.getAndSet(true);
          // eventList reached - back out and return
          return Optional.empty();
        } else if (!"epcisBody".equals(fieldName) && fieldName != null) {
          final JsonNode o = jsonParser.readValueAsTree();
          if (o != null) {
            header.set(fieldName, o);
          }
        }
      }
      if (token == null) {
        jsonParser.close();
      }
    } catch (Exception e) {
      return Optional.of(e);
    }
    return Optional.empty();
  }

  /**
   * ObjectNodes from eventList are to be ignored
   *
   * @return true if eventList is ignored
   */
  public boolean isEventListIgnored() {
    return ignoreEventList.get();
  }

  public class ObjectNodeSubscription implements Subscription {

    private final AtomicBoolean isTerminated = new AtomicBoolean(false);

    private final AtomicLong demand = new AtomicLong();

    private final AtomicReference<Subscriber<? super T>> subscriber;

    private ObjectNodeSubscription(Subscriber<? super T> subscriber) {
      if (subscriber == null) {
        throw new NullPointerException("subscriber must not be null");
      }
      this.subscriber = new AtomicReference<>(subscriber);
    }

    @Override
    public void request(long l) {
      if (l <= 0 && !terminate()) {
        subscriber.get().onError(new IllegalArgumentException("negative subscription request"));
        return;
      }

      if (demand.get() > 0) {
        demand.getAndAdd(l);
        return;
      }
      demand.getAndAdd(l);

      try {
        while (demand.get() > 0 && !isTerminated()) {
          final long count = readNext(demand.get());
          if (count >= 0) {
            demand.getAndAdd(-1 * count);
            nodeCount.getAndAdd(count);
          } else if (!terminate()) {
            subscriber.get().onComplete();
            return;
          }
        }
      } catch (Exception ex) {
        if (!terminate()) {
          subscriber.get().onError(ex);
        }
      }
    }

    @Override
    public void cancel() {
      terminate();
      subscriber.set(null);
    }

    private boolean terminate() {
      return isTerminated.getAndSet(true);
    }

    private boolean isTerminated() {
      return isTerminated.get();
    }

    /**
     * read next nodes from json parser and publish as many as requested
     *
     * @param requested number of nodes requested by subscriber
     * @return number of nodes published to subscriber
     * @throws IOException exception while reading
     */
    private long readNext(final long requested) throws IOException {
      long l = publishValidHeaderNode(requested);
      l += readEventList(requested - l);
      l += processEOF(requested - l);
      // return -1 if no nodes have published and no more nodes can be expected
      return l > 0 || isTokenAvailable() ? l : -1;
    }

    /**
     * read next nodes from eventList
     *
     * @param requested number of requested node
     * @return number of nodes published
     * @throws IOException in case of error while reading
     */
    private long readEventList(final long requested) throws IOException {
      if (!inEventList.get() || requested == 0) {
        return 0;
      }
      // skip eventList if requested
      while (isEventListIgnored() && isTokenAvailable()) {
        if (token == JsonToken.END_ARRAY) {
          return 0;
        }
        token = jsonParser.nextToken();
      }
      long l = 0;
      while (!isEventListIgnored()
          && isTokenAvailable()
          && token == JsonToken.START_OBJECT
          && l < requested) {
        JsonNode o = jsonParser.readValueAsTree();
        if (o.has("type")) {
          l++;
          subscriber.get().onNext((T) o);
        }
        token = jsonParser.nextToken();
      }
      // move forward to end of epcisBody
      if (token == JsonToken.END_ARRAY) {
        inEventList.getAndSet(false);
        jsonParser.nextToken();
        token = jsonParser.nextToken();
      }
      return l;
    }

    /**
     * publish header node if it's a valid EPCISDocument node
     *
     * @param requested number of requested node
     * @return number of nodes published
     */
    private long publishValidHeaderNode(final long requested) {
      if (requested > 0
          && !headerSent.get()
          && ((!isTokenAvailable() && ObjectNodeUtil.isValidEPCISDocumentNode(header))
              || (isTokenAvailable()
                  && nodeCount.get() == 0
                  && ObjectNodeUtil.isValidEPCISDocumentNode(header)))) {
        headerSent.getAndSet(true);
        subscriber.get().onNext((T) header);
        return 1;
      }
      return 0;
    }

    /**
     * read additional data after eventList if header node hasn't been published yet, publish it to
     * the subscriber
     *
     * @param requested number of requested node
     * @return number of nodes published
     * @throws IOException in case of error while reading
     */
    private synchronized long processEOF(final long requested) throws IOException {
      if (requested == 0) {
        return 0;
      }
      if (isTokenAvailable() && token == JsonToken.END_OBJECT
          || token == JsonToken.END_ARRAY
          || token == JsonToken.FIELD_NAME) {
        appendHeaderFields();
        token = jsonParser.nextToken();
      }
      return publishValidHeaderNode(requested);
    }

    /**
     * append additional fields to unpublished header node
     *
     * @throws IOException in case of error while reading
     */
    private void appendHeaderFields() throws IOException {
      while (isTokenAvailable() && token != JsonToken.END_OBJECT) {
        if (!headerSent.get()) {
          final String fieldName = jsonParser.nextFieldName();
          final JsonNode j = jsonParser.readValueAsTree();
          if (j != null) {
            header.set(fieldName != null ? fieldName : jsonParser.getCurrentName(), j);
          }
        }
        token = jsonParser.nextToken();
      }
    }

    /**
     * check if token is available and JsonParser is still active
     *
     * @return true when more tokens are expected
     */
    private boolean isTokenAvailable() {
      return !jsonParser.isClosed();
    }
  }
}
