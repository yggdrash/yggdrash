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
import io.yggdrash.common.exception.FailedOperationException;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.digests.KeccakDigest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.util.DigestFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import static java.util.Arrays.copyOfRange;

public class HashUtil {

    private HashUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static final String HASH_256_ALGORITHM_NAME = "KECCAK-256";
    private static final String HASH_SHA_256_ALGORITHM_NAME = "SHA-256";
    private static final String HASH_SHA_512_ALGORITHM_NAME = "SHA-512";
    private static final String HASH_SHA3_256_ALGORITHM_NAME = "SHA3-256";
    private static final String HASH_SHA3_512_ALGORITHM_NAME = "SHA3-512";
    private static final String HASH_SHA_1_ALGORITHM_NAME = "SHA-1";

    static {
        if (Security.getProvider("SC") == null) {
            Security.addProvider(SpongyCastleProvider.getInstance());
        }
    }

    /**
     * SHA3(Keccak256) Hash Method.
     *
     * @param input data
     * @return hashed data
     */
    public static byte[] sha3(byte[] input) {
        return hash(input, HASH_256_ALGORITHM_NAME);
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
     * SHA1 Hash Method.
     *
     * @param input data
     * @return hashed data
     */
    public static byte[] sha1(byte[] input) {
        return hash(input, HASH_SHA_1_ALGORITHM_NAME);
    }

    /**
     * SHA256 Hash Method.
     *
     * @param input - data for hashing
     * @return - sha256 hash of the data
     */
    static byte[] sha256(byte[] input) {
        return hash(input, HASH_SHA_256_ALGORITHM_NAME);
    }

    /**
     * The hash method for supporting many algorithms.
     *
     * @param input     data for hashing.
     * @param algorithm algorithm for hashing. ex) "KECCAK-256", "SHA-256", "SHA3-256", "SHA-1"
     * @return hashed data.
     */
    public static byte[] hash(byte[] input, String algorithm) {
        return hash(input, algorithm, false);
    }

    /**
     * The hash method for supporting many algorithms.
     *
     * @param input      data for hashing.
     * @param algorithm  algorithm for hashing. ex) "KECCAK-256", "SHA-256", "SHA3-256", "SHA-1"
     * @param doubleHash whether double hash or not
     * @return hashed data.
     */
    public static byte[] hash(byte[] input, String algorithm, boolean doubleHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            return doubleHash ? digest.digest(digest.digest(input)) : digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new FailedOperationException(e);
        }
    }

    /**
     * Get pbkdf2's hash encrypted output(key).
     *
     * @param input input data ex) password
     * @param salt random bytes
     * @param iteration iteration
     * @param outLen output length(bytes) ex) hash encrypted password
     * @param algorithm hash algorithm
     *                  ex) "KECCAK-256", "SHA-256", "SHA-512", "SHA3-256", "SHA3-512", "SHA-1"
     * @return encrypted output(key)
     */
    public static byte[] pbkdf2(
            byte[] input, byte[] salt, int iteration, int outLen, String algorithm) {
        Digest digest;

        switch (algorithm) {
            case HASH_256_ALGORITHM_NAME:
                digest = new KeccakDigest(256);
                break;

            case HASH_SHA_256_ALGORITHM_NAME:
                digest = DigestFactory.createSHA256();
                break;

            case HASH_SHA_512_ALGORITHM_NAME:
                digest = DigestFactory.createSHA512();
                break;

            case HASH_SHA3_256_ALGORITHM_NAME:
                digest = DigestFactory.createSHA3_256();
                break;

            case HASH_SHA3_512_ALGORITHM_NAME:
                digest = DigestFactory.createSHA3_512();
                break;

            case HASH_SHA_1_ALGORITHM_NAME:
                digest = DigestFactory.createSHA1();
                break;

            default:
                throw new IllegalStateException("unknown digest for PBKDF2");
        }

        PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(digest);
        gen.init(input, salt, iteration);
        int keyLengthInBits = outLen * 8;
        CipherParameters p = gen.generateDerivedParameters(keyLengthInBits);
        return ((KeyParameter) p).getKey();
    }
}
