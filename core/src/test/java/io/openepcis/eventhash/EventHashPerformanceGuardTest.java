/*
 * Copyright 2022-2026 benelog GmbH & Co. KG
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
package io.openepcis.eventhash;

import io.openepcis.constants.CBVVersion;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lightweight CI performance guard for the digest hot path.
 */
class EventHashPerformanceGuardTest {

    // A representative pre-hash string for the isolated digest benchmark (mirrors canonical-form output).
    private static final String PREHASH_STRING = "eventType=ObjectEventeventTime=2005-04-04T02:33:31.116ZeventTimeZoneOffset=-06:00epcListepc=https://id.gs1.org/01/10614141073464/21/2017epc=https://id.gs1.org/01/10614141073464/21/2018action=OBSERVEbizStep=https://ref.gs1.org/cbv/BizStep-shippingdisposition=https://ref.gs1.org/cbv/Disp-in_transitreadPointid=https://id.gs1.org/414/0614141073467/254/1234bizTransactionListtype=https://ref.gs1.org/cbv/BTT-pobizTransaction=http://transaction.acme.com/po/12345678";
    private static final int WARMUP_ITERATIONS = 20_000;
    private static final int MEASURED_ITERATIONS = 200_000;   // ~0.4s expected at ~2us/op
    private static final long CEILING_MILLIS = 5_000;         // generous: ~10x headroom; catches only catastrophic blowups

    @Test
    void digestHotPathStaysFast() throws NoSuchAlgorithmException {
        // Warm up: run untimed so HotSpot compiles the method to native code
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            HashIdGenerator.generateHashId(PREHASH_STRING, "sha-256", CBVVersion.VERSION_2_0_0);
        }

        // Measure: time the hot path over many iterations.
        final long startNanos = System.nanoTime();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            HashIdGenerator.generateHashId(PREHASH_STRING, "sha-256", CBVVersion.VERSION_2_0_0);
        }
        final long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

        // Generous ceiling: passes comfortably today, trips only on a large (>10x) regression.
        assertTrue(
                elapsedMillis < CEILING_MILLIS,
                "Digest hot path regressed: " + MEASURED_ITERATIONS + " hashes took " + elapsedMillis
                        + "ms (ceiling " + CEILING_MILLIS + "ms). A large regression likely reintroduced "
                        + "per-event allocation or blocking work — check the HashIdGenerator MessageDigest "
                        + "cache and EventHashGenerator Pattern cache, then run the JMH benchmark for detail.");
    }
}
