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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/**
 * Generates EPCIS canonical hash IDs ({@code ni:///<algo>;<hex>?ver=CBV<version>}) from a pre-hash string.
 * Algorithm registry is data-driven ({@code ALGORITHMS} map); unknown names fall back to sha-256.
 * Per-thread {@link MessageDigest} cache keeps repeated calls allocation-free.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HashIdGenerator {

    /**
     * Pairs a user-facing algorithm name with its EPCIS URI prefix and JCA digest name.
     */
    private record AlgorithmSpec(String uriPrefix, String jcaName) {
    }

    // Algorithm registry: lowercase user-facing name -> (URI prefix, JCA name).
    private static final Map<String, AlgorithmSpec> ALGORITHMS = Map.ofEntries(
            Map.entry("sha-1", new AlgorithmSpec("ni:///sha-1;", "SHA-1")),
            Map.entry("sha-224", new AlgorithmSpec("ni:///sha-224;", "SHA-224")),
            Map.entry("sha-256", new AlgorithmSpec("ni:///sha-256;", "SHA-256")),
            Map.entry("sha-384", new AlgorithmSpec("ni:///sha-384;", "SHA-384")),
            Map.entry("sha-512", new AlgorithmSpec("ni:///sha-512;", "SHA-512")),
            Map.entry("sha3-224", new AlgorithmSpec("ni:///sha3-224;", "SHA3-224")),
            Map.entry("sha3-256", new AlgorithmSpec("ni:///sha3-256;", "SHA3-256")),
            Map.entry("sha3-384", new AlgorithmSpec("ni:///sha3-384;", "SHA3-384")),
            Map.entry("sha3-512", new AlgorithmSpec("ni:///sha3-512;", "SHA3-512")),
            Map.entry("md2", new AlgorithmSpec("ni:///md2;", "MD2")),
            Map.entry("md5", new AlgorithmSpec("ni:///md5;", "MD5"))
    );

    // Fallback when caller passes an unknown algorithm name — preserves prior default-branch behavior.
    private static final AlgorithmSpec DEFAULT_ALGORITHM = ALGORITHMS.get("sha-256");

    // Lowercase hex formatter — stdlib, single allocation per call.
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    // MessageDigest is not thread-safe, so cache one instance per JCA name per thread.
    private static final ThreadLocal<Map<String, MessageDigest>> DIGEST_CACHE = ThreadLocal.withInitial(HashMap::new);

    /**
     * Returns a thread-local {@link MessageDigest} for the given JCA name, instantiating once per thread.
     */
    private static MessageDigest digestFor(final String jcaName) throws NoSuchAlgorithmException {
        final Map<String, MessageDigest> cache = DIGEST_CACHE.get();
        MessageDigest md = cache.get(jcaName);
        if (md == null) {
            md = MessageDigest.getInstance(jcaName);
            cache.put(jcaName, md);
        }
        return md;
    }

    /**
     * Generates the EPCIS hash-id for a pre-hash string.
     *
     * <p>Returned URI form: {@code ni:///<algo>;<hex-digest>?ver=CBV<version>}.
     * Unknown {@code hashAlgorithm} falls back to sha-256; unknown {@code cbvVersion} falls back to the default version behaviour (see {@link CbvBehavior#DEFAULT_VERSION}).
     */
    public static String generateHashId(final String preHashString,
                                        final String hashAlgorithm,
                                        final CBVVersion cbvVersion) throws NoSuchAlgorithmException {
        final AlgorithmSpec spec = ALGORITHMS.getOrDefault(hashAlgorithm.toLowerCase(), DEFAULT_ALGORITHM);
        final String cbvSuffix = CbvBehavior.of(cbvVersion).hashUriSuffix();

        final byte[] digest = digestFor(spec.jcaName()).digest(preHashString.getBytes(StandardCharsets.UTF_8));
        return spec.uriPrefix() + HEX_FORMAT.formatHex(digest) + cbvSuffix;
    }
}
