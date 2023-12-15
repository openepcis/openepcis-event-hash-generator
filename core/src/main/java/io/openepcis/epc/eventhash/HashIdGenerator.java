/*
 * Copyright 2022-2023 benelog GmbH & Co. KG
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
package io.openepcis.epc.eventhash;

import io.openepcis.constants.CBVVersion;
import jakarta.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

// Class that will generate the Hash-id for the event based on the provided pre-hash string and type
// of hash algorithm
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HashIdGenerator {

  // Method which accepts the pre-hash string for which SHA-256 hash-id needs to be created.
  public static String generateHashId(final String preHashString, final String hashAlgorithm, final CBVVersion cbvVersion)
      throws NoSuchAlgorithmException {
    // Create the StringBuilder to append prefix, hash and suffix according to required EPCIS
    // standard.
    final StringBuilder hashId = new StringBuilder();
    byte[] digest;

    // Based on the type of algorithm specified by the user, create respective type of Hash IDs.
    switch (hashAlgorithm.toLowerCase()) {
      case "sha-1" -> {
        hashId.append("ni:///sha-1;");
        digest =
            MessageDigest.getInstance("SHA-1")
                .digest(preHashString.getBytes(StandardCharsets.UTF_8));
        hashId.append(DatatypeConverter.printHexBinary(digest).toLowerCase());
      }
      case "sha-224" -> {
        hashId.append("ni:///sha-224;");
        digest =
            MessageDigest.getInstance("SHA-224")
                .digest(preHashString.getBytes(StandardCharsets.UTF_8));
        hashId.append(DatatypeConverter.printHexBinary(digest).toLowerCase());
      }
      case "sha-384" -> {
        hashId.append("ni:///sha-384;");
        digest =
            MessageDigest.getInstance("SHA-384")
                .digest(preHashString.getBytes(StandardCharsets.UTF_8));
        hashId.append(DatatypeConverter.printHexBinary(digest).toLowerCase());
      }
      case "sha-512" -> {
        hashId.append("ni:///sha-512;");
        digest =
            MessageDigest.getInstance("SHA-512")
                .digest(preHashString.getBytes(StandardCharsets.UTF_8));
        hashId.append(DatatypeConverter.printHexBinary(digest).toLowerCase());
      }
      case "sha3-224" -> {
        hashId.append("ni:///sha3-224;");
        digest =
            MessageDigest.getInstance("SHA3-224")
                .digest(preHashString.getBytes(StandardCharsets.UTF_8));
        hashId.append(DatatypeConverter.printHexBinary(digest).toLowerCase());
      }
      case "sha3-256" -> {
        hashId.append("ni:///sha3-256;");
        digest =
            MessageDigest.getInstance("SHA3-256")
                .digest(preHashString.getBytes(StandardCharsets.UTF_8));
        hashId.append(DatatypeConverter.printHexBinary(digest).toLowerCase());
      }
      case "sha3-384" -> {
        hashId.append("ni:///sha3-384;");
        digest =
            MessageDigest.getInstance("SHA3-384")
                .digest(preHashString.getBytes(StandardCharsets.UTF_8));
        hashId.append(DatatypeConverter.printHexBinary(digest).toLowerCase());
      }
      case "sha3-512" -> {
        hashId.append("ni:///sha3-512;");
        digest =
            MessageDigest.getInstance("SHA3-512")
                .digest(preHashString.getBytes(StandardCharsets.UTF_8));
        hashId.append(DatatypeConverter.printHexBinary(digest).toLowerCase());
      }
      case "md2" -> {
        hashId.append("ni:///md2;");
        digest =
            MessageDigest.getInstance("MD2").digest(preHashString.getBytes(StandardCharsets.UTF_8));
        hashId.append(DatatypeConverter.printHexBinary(digest).toLowerCase());
      }
      case "md5" -> {
        hashId.append("ni:///md5;");
        digest =
            MessageDigest.getInstance("MD5").digest(preHashString.getBytes(StandardCharsets.UTF_8));
        hashId.append(DatatypeConverter.printHexBinary(digest).toLowerCase());
      }
      default -> {
        hashId.append("ni:///sha-256;");
        digest =
            MessageDigest.getInstance("SHA-256")
                .digest(preHashString.getBytes(StandardCharsets.UTF_8));
        hashId.append(DatatypeConverter.printHexBinary(digest).toLowerCase());
      }
    }

    return hashId.append("?ver=")
            .append(cbvVersion.equals(CBVVersion.VERSION_2_0_0) ? "CBV2.0" : "CBV2.1")
            .toString();
  }
}
