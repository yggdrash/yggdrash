/* CHECKSTYLE:OFF
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package io.yggdrash.common.crypto;

import io.yggdrash.common.crypto.jce.SpongyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

import static java.util.Arrays.copyOfRange;

public class HashUtil {

    private static final Logger LOG = LoggerFactory.getLogger(HashUtil.class);

    private static final Provider CRYPTO_PROVIDER;

    private static final String HASH_256_ALGORITHM_NAME;

    private static final String HASH_SHA_1_ALGORITHM_NAME;

    static {

        Security.addProvider(SpongyCastleProvider.getInstance());

        CRYPTO_PROVIDER = Security.getProvider("SC");
        HASH_256_ALGORITHM_NAME = "ETH-KECCAK-256";
        HASH_SHA_1_ALGORITHM_NAME = "SHA-1";
    }

    /**
     * SHA256 Hash Method.
     * @param input - data for hashing
     * @return - sha256 hash of the data
     * @deprecated Use hash()
     */
    public static byte[] sha256(byte[] input) {
        try {
            MessageDigest sha256digest = MessageDigest.getInstance("SHA-256");
            return sha256digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Can't find such algorithm", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * SHA3(Keccak256) Hash Method.
     *
     * @param input data
     * @return hashed data
     * @deprecated Use hash()
     */
    public static byte[] sha3(byte[] input) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME, CRYPTO_PROVIDER);
            digest.update(input);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Can't find such algorithm", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * SHA1 Hash Method.
     *
     * @param input data
     * @return hashed data
     * @deprecated Use hash()
     */
    public static byte[] sha1(byte[] input) {
        try {
            MessageDigest sha256digest = MessageDigest.getInstance(HASH_SHA_1_ALGORITHM_NAME);
            return sha256digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Can't find such algorithm", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Calculates RIGTMOST160(SHA3(input)). This is used in address
     * calculations. *
     *
     * @param input - data
     * @return - 20 right bytes of the hash keccak of the data
     */
    public static byte[] sha3omit12(byte[] input) {
        byte[] hash = sha3(input);
        return copyOfRange(hash, 12, hash.length);
    }

    /**
     * The hash method for supporting many algorithms.
     *
     * @param input      data for hashing.
     * @param algorithm  algorithm for hashing. ex) "SHA-256", "KECCAK-256", "SHA3-256", "SHA-1"
     * @param doubleHash whether double hash or not
     * @return hashed data.
     */
    public static byte[] hash(byte[] input, String algorithm, boolean doubleHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            return doubleHash ? digest.digest(digest.digest(input)) : digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Can't find such algorithm", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * The hash method for supporting many algorithms.
     *
     * @param input     data for hashing.
     * @param algorithm algorithm for hashing. ex) "SHA-256", "KECCAK-256", "SHA3-256", "SHA-1"
     * @return hashed data.
     */
    public static byte[] hash(byte[] input, String algorithm) {
        return hash(input, algorithm, false);
    }

    /**
     * The hash method for KECCAK-256.
     *
     * @param input data for hashing.
     * @return hashed data.
     */
    public static byte[] hash(byte[] input) {
        return hash(input, "KECCAK-256");
    }

}
