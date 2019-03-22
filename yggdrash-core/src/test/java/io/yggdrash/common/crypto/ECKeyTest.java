/*
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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.yggdrash.common.crypto.ECKey.ECDSASignature;
import io.yggdrash.common.crypto.jce.SpongyCastleProvider;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.util.List;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ECKeyTest {
    private static final Logger log = LoggerFactory.getLogger(ECKeyTest.class);

    private static final SecureRandom secureRandom = new SecureRandom();

    private final String privString = "c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4";
    private final BigInteger privateKey = new BigInteger(privString, 16);

    private final String pubString = "040947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc8ad75aa17564ae80a20bb044ee7a6d903e8e8df624b089c95d66a0570f051e5a05b";
    private final String compressedPubString = "030947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc8ad";
    private final byte[] pubKey = Hex.decode(pubString);
    private final byte[] compressedPubKey = Hex.decode(compressedPubString);
    private final String address = "cd2a3d9f938e13cd947ec05abc7fe734df8dd826";

    private final String exampleMessage = "This is an example of a signed message.";
    private final String sigBase64 = "HNLOSI9Nop5o8iywXKwbGbdd8XChK0rRvdRTG46RFcb7dcH+UKlejM/8u1SCoeQvu91jJBMd/nXDs7f5p8ch7Ms=";
    private final String signatureHex = "d2ce488f4da29e68f22cb05cac1b19b75df170a12b4ad1bdd4531b8e9115c6fb75c1fe50a95e8ccffcbb5482a1e42fbbdd6324131dfe75c3b3b7f9a7c721eccb01";

    @Test
    public void testHashCode() {
        Assert.assertEquals(-351262686, ECKey.fromPrivate(privateKey).hashCode());
    }

    @Test
    public void testECKey() {
        ECKey key = new ECKey();
        assertTrue(key.isPubKeyCanonical());
        assertNotNull(key.getPubKey());
        assertNotNull(key.getPrivKeyBytes());
        log.debug(Hex.toHexString(key.getPrivKeyBytes()) + " :Generated privkey");
        log.debug(Hex.toHexString(key.getPubKey()) + " :Generated pubkey");
    }

    @Test
    public void testFromPrivateKey() {
        ECKey key = ECKey.fromPrivate(privateKey);
        assertTrue(key.isPubKeyCanonical());
        assertTrue(key.hasPrivKey());
        assertArrayEquals(pubKey, key.getPubKey());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrivatePublicKeyBytesNoArg() {
        // Expecting an IllegalArgumentException for using only null-parameters
        new ECKey((BigInteger) null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPrivateKey() throws Exception {
        // Expecting an IllegalArgumentException for using an non EC private key
        PrivateKey privateKey = KeyPairGenerator.getInstance("DSA").generateKeyPair().getPrivate();
        new ECKey(
                Security.getProvider("SunEC"),
                privateKey,
                ECKey.fromPublicOnly(pubKey).getPubKeyPoint());
    }

    @Test
    public void testIsPubKeyOnly() {
        ECKey key = ECKey.fromPublicOnly(pubKey);
        assertTrue(key.isPubKeyCanonical());
        assertTrue(key.isPubKeyOnly());
        assertArrayEquals(key.getPubKey(), pubKey);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSignIncorrectInputSize() {
        // Expecting an IllegalArgumentException for a non 32-byte input
        ECKey key = new ECKey();
        String message = "The quick brown fox jumps over the lazy dog.";
        key.doSign(message.getBytes());
    }

    @Test(expected = ECKey.MissingPrivateKeyException.class)
    public void testSignWithPubKeyOnly() {
        // Expecting an MissingPrivateKeyException for a public only ECKey
        ECKey key = ECKey.fromPublicOnly(pubKey);
        String message = "The quick brown fox jumps over the lazy dog.";
        byte[] input = HashUtil.sha3(message.getBytes());
        key.doSign(input);
    }

    @Test(expected = SignatureException.class)
    public void testBadBase64Sig() throws SignatureException {
        // Expecting a SignatureException for invalid Base64
        byte[] messageHash = new byte[32];
        ECKey.signatureToKey(messageHash, "This is not valid Base64!");
    }

    @Test(expected = SignatureException.class)
    public void testInvalidSignatureLength() throws SignatureException {
        // Expecting a SignatureException for invalid signature length
        byte[] messageHash = new byte[32];
        ECKey.signatureToKey(messageHash, "abcdefg");
    }

    @Test
    public void testPublicKeyFromPrivate() {
        byte[] pubFromPriv = ECKey.publicKeyFromPrivate(privateKey, false);
        assertArrayEquals(pubKey, pubFromPriv);
    }

    @Test
    public void testPublicKeyFromPrivateCompressed() {
        byte[] pubFromPriv = ECKey.publicKeyFromPrivate(privateKey, true);
        assertArrayEquals(compressedPubKey, pubFromPriv);
    }

    @Test
    public void testGetAddress() {
        ECKey key = ECKey.fromPublicOnly(pubKey);
        assertArrayEquals(Hex.decode(address), key.getAddress());
    }

    @Test
    public void testToString() {
        ECKey key = ECKey.fromPrivate(BigInteger.TEN); // An example private key.
        assertEquals("pub:04a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7", key.toString());
    }

    @Test
    public void testEthereumSign() {
        ECKey key = ECKey.fromPrivate(privateKey);
        log.debug("Secret\t: " + Hex.toHexString(key.getPrivKeyBytes()));
        log.debug("Pubkey\t: " + Hex.toHexString(key.getPubKey()));
        log.debug("Data\t: " + exampleMessage);
        byte[] messageHash = HashUtil.sha3(exampleMessage.getBytes());
        ECDSASignature signature = key.sign(messageHash);
        String output = signature.toBase64();
        log.debug("Sign\t: " + output + " (Base64, length: " + output.length() + ")");
        assertEquals(sigBase64, output);
    }

    /**
     * Verified via https://etherchain.org/verify/signature
     */
    @Test
    public void testEthereumSignToHex() {
        ECKey key = ECKey.fromPrivate(privateKey);
        byte[] messageHash = HashUtil.sha3(exampleMessage.getBytes());
        ECDSASignature signature = key.sign(messageHash);
        String output = signature.toHex();
        log.debug("Signature\t: " + output + " (Hex, length: " + output.length() + ")");

        assertEquals(signatureHex, output);
    }

    @Test
    public void testVerifySignature1() {
        ECKey key = ECKey.fromPublicOnly(pubKey);
        BigInteger r = new BigInteger("95350169487015575001444507567851457309689354890536757640816472151471942911739");
        BigInteger s = new BigInteger("53263359985948886716508128220321578640585687230908938312863706887350789467339");
        ECDSASignature sig = ECDSASignature.fromComponents(r.toByteArray(), s.toByteArray(), (byte) 28);

        assertTrue(key.verify(HashUtil.sha3(exampleMessage.getBytes()), sig));
    }

    @Test
    public void testVerifySignature2() throws SignatureException {
        BigInteger r = new BigInteger("c52c114d4f5a3ba904a9b3036e5e118fe0dbb987fe3955da20f2cd8f6c21ab9c", 16);
        BigInteger s = new BigInteger("6ba4c2874299a55ad947dbc98a25ee895aabf6b625c26c435e84bfd70edf2f69", 16);
        ECDSASignature sig = ECDSASignature.fromComponents(r.toByteArray(), s.toByteArray(), (byte) 0x1b);
        byte[] rawtx = Hex.decode("f82804881bc16d674ec8000094cd2a3d9f938e13cd947ec05abc7fe734df8dd8268609184e72a0006480");
        byte[] rawHash = HashUtil.sha3(rawtx);
        byte[] address = Hex.decode("cd2a3d9f938e13cd947ec05abc7fe734df8dd826");
        ECKey key = ECKey.signatureToKey(rawHash, sig);

        assertEquals(key, ECKey.signatureToKey(rawHash, sig.toBase64()));
        assertEquals(key, ECKey.recoverFromSignature(0, sig, rawHash));
        assertArrayEquals(key.getPubKey(), ECKey.recoverPubBytesFromSignature(0, sig, rawHash));

        assertArrayEquals(address, key.getAddress());
        assertArrayEquals(address, ECKey.signatureToAddress(rawHash, sig));
        assertArrayEquals(address, ECKey.signatureToAddress(rawHash, sig.toBase64()));
        assertArrayEquals(address, ECKey.recoverAddressFromSignature(0, sig, rawHash));

        assertTrue(key.verify(rawHash, sig));
    }

    @Test // result is a point at infinity
    public void testVerifySignature4() throws SignatureException {

        byte[] hash = Hex.decode("6ee854b88dbf19846c58fd2beac582adcf28cfdd2b3a8427ac29d7b70153d35c");
        BigInteger r = new BigInteger("95350169487015575001444507567851457309689354890536757640816472151471942911739");
        BigInteger s = new BigInteger("53263359985948886716508128220321578640585687230908938312863706887350789467339");
        ECDSASignature sig = ECDSASignature.fromComponents(r.toByteArray(), s.toByteArray(), (byte) 28);
        ECKey key = ECKey.signatureToKey(hash, sig);

        assertTrue(key.verify(hash, sig));
    }

    @Test
    public void testVerifySignature5() {
        ECKey key = ECKey.fromPrivate(privateKey);
        byte[] messageHash = HashUtil.sha3(exampleMessage.getBytes());
        ECDSASignature signature = key.sign(messageHash);

        assertTrue(key.verify(messageHash, signature));
    }

    @Test
    public void testVerifySignature6() throws SignatureException {
        // generate ECkey object with privateKey
        ECKey key = ECKey.fromPrivate(privateKey);

        // check public key with pubKey
        assertArrayEquals(pubKey, key.getPubKey());

        // generate messageHash with exampleMessage
        byte[] messageHash = HashUtil.sha3(exampleMessage.getBytes());

        // generate ECDSASignature with ECKey, messageHash
        ECDSASignature signature = key.sign(messageHash);

        // verify the sign message
        assertTrue(key.verify(messageHash, signature));

        // get public key with messageHash, ECDSASignature
        ECKey keyFromSig = ECKey.signatureToKey(messageHash, signature);
        byte[] pubKeyFromSig = keyFromSig.getPubKey();
        assertArrayEquals(pubKey, pubKeyFromSig);

        // verify the sign message
        assertTrue(keyFromSig.verify(messageHash, signature));
    }

    @Test
    public void testVerifySignature_Static() throws SignatureException {

        byte[] messageHash = Hex.decode("92e0d4290bba01aa0abbb4705360c751af13fdb1131b8f6f1e632c4621adac75");
        byte[] signature = Hex.decode("1cca588a8eb84d5bf6741bc6e0ccfbe1fdb05b6c624b5fe72199fa1f2e501f876c5b5f11863323a998b79a0d27714fcc8825cf357903e863396f2e2e281220de31");

        // get public key with messageHash, ECDSASignature
        ECKey.ECDSASignature sig = new ECKey.ECDSASignature(signature);
        ECKey keyFromSig = ECKey.signatureToKey(messageHash, sig);
        log.debug("address=" + Hex.toHexString(keyFromSig.getAddress()));

        byte[] pubKeyFromSig = keyFromSig.getPubKey();
        log.debug("pubKey=" + Hex.toHexString(pubKeyFromSig));

        assertArrayEquals(pubKeyFromSig, Hex.decode("0493fe448d38c77c212cce10c07ed37984c59bedac51219b70847429153063cfae0d2f42ba394ffe9d5d2d11b0c0f400ac04997c584c0ef6f2041cf20f8c2c446b"));

        // verify the sign message
        assertTrue(keyFromSig.verify(messageHash, sig));

    }

    @Test
    @SuppressWarnings("UnstableApiUsage")
    public void testSValue() throws Exception {
        // Check that we never generate an S value that is larger than half the curve order. This avoids a malleability
        // issue that can allow someone to change a transaction [hash] without invalidating the signature.
        final int ITERATIONS = 10;
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(ITERATIONS));
        List<ListenableFuture<ECKey.ECDSASignature>> sigFutures = Lists.newArrayList();
        final ECKey key = new ECKey();
        for (byte i = 0; i < ITERATIONS; i++) {
            final byte[] hash = HashUtil.sha3(new byte[] {i});
            sigFutures.add(executor.submit(() -> key.doSign(hash)));
        }
        List<ECKey.ECDSASignature> sigs = Futures.allAsList(sigFutures).get();
        for (ECKey.ECDSASignature signature : sigs) {
            assertTrue(signature.s.compareTo(ECKey.HALF_CURVE_ORDER) <= 0);
        }
        final ECKey.ECDSASignature duplicate = new ECKey.ECDSASignature(sigs.get(0).r, sigs.get(0).s);
        assertEquals(sigs.get(0), duplicate);
        assertEquals(sigs.get(0).hashCode(), duplicate.hashCode());
    }

    @Test
    public void testSignVerify() {
        ECKey key = ECKey.fromPrivate(privateKey);
        String message = "This is an example of a signed message.";
        byte[] input = HashUtil.sha3(message.getBytes());
        ECDSASignature sig = key.sign(input);
        assertTrue(sig.validateComponents());
        assertTrue(key.verify(input, sig));
    }

    private void testProviderRoundTrip(Provider provider) {
        ECKey key = new ECKey(provider, secureRandom);
        String message = "The quick brown fox jumps over the lazy dog.";
        byte[] input = HashUtil.sha3(message.getBytes());
        ECDSASignature sig = key.sign(input);
        assertTrue(sig.validateComponents());
        assertTrue(key.verify(input, sig));
    }

    @Test
    public void testSunECRoundTrip() {
        Provider provider = Security.getProvider("SunEC");
        if (provider != null) {
            try {
                testProviderRoundTrip(provider);
            } catch (Throwable t) {
                log.debug(t.getMessage());
            }
        } else {
            log.debug("Skip test as provider doesn't exist. " +
                    "Must be OpenJDK which could be shipped without 'SunEC'");
        }
    }

    @Test
    public void testSpongyCastleRoundTrip() {
        testProviderRoundTrip(SpongyCastleProvider.getInstance());
    }

    @Test
    public void testIsPubKeyCanonicalCorect() {
        // Test correct prefix 4, right length 65
        byte[] canonicalPubkey1 = new byte[65];
        canonicalPubkey1[0] = 0x04;
        assertTrue(ECKey.isPubKeyCanonical(canonicalPubkey1));
        // Test correct prefix 2, right length 33
        byte[] canonicalPubkey2 = new byte[33];
        canonicalPubkey2[0] = 0x02;
        assertTrue(ECKey.isPubKeyCanonical(canonicalPubkey2));
        // Test correct prefix 3, right length 33
        byte[] canonicalPubkey3 = new byte[33];
        canonicalPubkey3[0] = 0x03;
        assertTrue(ECKey.isPubKeyCanonical(canonicalPubkey3));
    }

    @Test
    public void testIsPubKeyCanonicalWrongLength() {
        // Test correct prefix 4, but wrong length !65
        byte[] nonCanonicalPubkey1 = new byte[64];
        nonCanonicalPubkey1[0] = 0x04;
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey1));
        // Test correct prefix 2, but wrong length !33
        byte[] nonCanonicalPubkey2 = new byte[32];
        nonCanonicalPubkey2[0] = 0x02;
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey2));
        // Test correct prefix 3, but wrong length !33
        byte[] nonCanonicalPubkey3 = new byte[32];
        nonCanonicalPubkey3[0] = 0x03;
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey3));
    }

    @Test
    public void testIsPubKeyCanonicalWrongPrefix() {
        // Test wrong prefix 4, right length 65
        byte[] nonCanonicalPubkey4 = new byte[65];
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey4));
        // Test wrong prefix 2, right length 33
        byte[] nonCanonicalPubkey5 = new byte[33];
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey5));
        // Test wrong prefix 3, right length 33
        byte[] nonCanonicalPubkey6 = new byte[33];
        assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey6));
    }

    @Test
    public void keyRecovery() {
        ECKey key = new ECKey();
        String message = "Hello World!";
        byte[] hash = HashUtil.sha256(message.getBytes());
        ECKey.ECDSASignature sig = key.doSign(hash);
        key = ECKey.fromPublicOnly(key.getPubKeyPoint());
        boolean found = false;
        for (int i = 0; i < 4; i++) {
            ECKey key2 = ECKey.recoverFromSignature(i, sig, hash);
            checkNotNull(key2);
            if (key.equals(key2)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testSignedMessageToKey() throws SignatureException {
        byte[] messageHash = HashUtil.sha3(exampleMessage.getBytes());
        ECKey key = ECKey.signatureToKey(messageHash, sigBase64);
        assertNotNull(key);
        assertArrayEquals(pubKey, key.getPubKey());
    }

    @Test
    public void testGetPrivKeyBytes() {
        ECKey key = new ECKey();
        assertNotNull(key.getPrivKeyBytes());
        assertEquals(32, key.getPrivKeyBytes().length);
    }

    @Test
    public void testEqualsObject() {
        ECKey key0 = new ECKey();
        ECKey key1 = ECKey.fromPrivate(privateKey);
        ECKey key2 = ECKey.fromPrivate(privateKey);

        assertNotEquals(key0, key1);
        assertEquals(key1, key1);
        assertEquals(key1, key2);
    }

    @Test
    public void testNodeId() {
        ECKey key = ECKey.fromPublicOnly(pubKey);

        assertEquals(key, ECKey.fromNodeId(key.getNodeId()));
    }
}
