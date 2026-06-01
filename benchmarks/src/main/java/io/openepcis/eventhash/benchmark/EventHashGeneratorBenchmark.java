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
package io.openepcis.eventhash.benchmark;

import io.openepcis.constants.CBVVersion;
import io.openepcis.eventhash.EventHashGenerator;
import io.openepcis.eventhash.HashIdGenerator;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * JMH microbenchmarks for the event-hash hot paths.
 *
 * <p>Run with:  java -jar benchmarks/target/benchmarks.jar
 * Quick run:    java -jar benchmarks/target/benchmarks.jar -wi 1 -i 2 -f 1
 *
 * <p>Measures three layers:
 * 1. fromXml        — full XML pipeline (SAX parse -> ContextNode -> canonicalize -> hash)
 * 2. fromJson       — full JSON pipeline (Jackson stream -> ContextNode -> canonicalize -> hash)
 * 3. generateHashId — isolated digest layer (MessageDigest cache + HexFormat + algorithm registry)
 */
@BenchmarkMode(Mode.AverageTime)                        // report average time per operation (intuitive "µs per hash")
@OutputTimeUnit(TimeUnit.MICROSECONDS)                  // use microseconds for better readability
@State(Scope.Thread)                                    // each thread gets its own instance (no shared state, no synchronization overhead)
@Warmup(iterations = 3, time = 1)                       // warm up with 3 iterations of 1 second each (JIT optimizations kick in)
@Measurement(iterations = 5, time = 1)                  // measure with 5 iterations of 1 second each (enough for stable results without excessive runtime)
@Fork(2)                                                // fork 2 separate JVMs to ensure clean environment and account for JVM warmup effects
public class EventHashGeneratorBenchmark {

    private static final String XML_FIXTURE = "2.0/EPCIS/XML/Capture/Documents/ObjectEvent.xml";
    private static final String JSON_FIXTURE = "2.0/EPCIS/JSON/Capture/Documents/ObjectEvent.json";

    // A representative pre-hash string for the isolated digest benchmark (mirrors canonical-form output).
    private static final String PREHASH_STRING = "eventType=ObjectEventeventTime=2005-04-04T02:33:31.116ZeventTimeZoneOffset=-06:00epcListepc=https://id.gs1.org/01/10614141073464/21/2017epc=https://id.gs1.org/01/10614141073464/21/2018action=OBSERVEbizStep=https://ref.gs1.org/cbv/BizStep-shippingdisposition=https://ref.gs1.org/cbv/Disp-in_transitreadPointid=https://id.gs1.org/414/0614141073467/254/1234bizTransactionListtype=https://ref.gs1.org/cbv/BTT-pobizTransaction=http://transaction.acme.com/po/12345678";

    private byte[] xmlBytes;
    private byte[] jsonBytes;
    private EventHashGenerator generator;


    // Runs once before the measured iterations — sets up immutable shared state
    @Setup
    public void setup() {
        try {
            xmlBytes = open(XML_FIXTURE);
            jsonBytes = open(JSON_FIXTURE);
        } catch (IOException e) {
            throw new RuntimeException("failed to load fixtures", e);
        }
        generator = new EventHashGenerator();
    }

    private byte[] open(final String resourcePath) throws IOException {
        final InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IllegalStateException("fixture not found on classpath: " + resourcePath);
        }
        // convert InputStream to byte[] and return
        try {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IOException("failed to read fixture: " + resourcePath, e);
        }
    }

    // Shut down Mutiny's worker pool after the trial. fromXml/fromJson offload to it via
    // runSubscriptionOn(...); its non-daemon threads otherwise keep the forked JVM alive,
    // making JMH wait 30s per fork before force-killing.
    @TearDown(Level.Trial)
    public void tearDown() {
        if (Infrastructure.getDefaultWorkerPool() instanceof ExecutorService es) {
            es.shutdownNow();
        }
    }

    // Benchmark 1: full XML pipeline. New stream per invocation since it gets consumed.
    @Benchmark
    public void fromXml(final Blackhole bh) {
        final List<Map<String, String>> hashes =
                generator
                        .fromXml(new ByteArrayInputStream(xmlBytes), "sha-256", "prehash")
                        .subscribe()
                        .asStream().toList();
        bh.consume(hashes);  // hand the result to JMH so the JIT can't delete the work as "unused"

    }

    // Benchmark 2: full JSON pipeline.
    @Benchmark
    public void fromJson(final Blackhole bh) throws IOException {
        final List<Map<String, String>> hashes =
                generator.fromJson(new ByteArrayInputStream(jsonBytes), Map.of(), "sha-256", "prehash")
                        .subscribe()
                        .asStream().toList();
        bh.consume(hashes);
    }

    // Benchmark 3: isolated digest layer — exercises the MessageDigest ThreadLocal cache,
    @Benchmark
    public void generateHashId(final Blackhole bh) throws NoSuchAlgorithmException {
        final String hash = HashIdGenerator.generateHashId(PREHASH_STRING, "sha-256", CBVVersion.VERSION_2_0_0);
        bh.consume(hash);
    }

}
