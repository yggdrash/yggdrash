/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.blockchain;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Timestamps;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SignatureException;
import java.util.Arrays;

import static io.yggdrash.common.config.Constants.KEY.BODY;
import static io.yggdrash.common.config.Constants.KEY.HEADER;
import static io.yggdrash.common.config.Constants.KEY.SIGNATURE;
import static io.yggdrash.common.config.Constants.TIMESTAMP_2018;
import static io.yggdrash.common.config.Constants.TX_BODY_MAX_LENGTH;
import static io.yggdrash.common.config.Constants.TX_HEADER_LENGTH;
import static io.yggdrash.common.config.Constants.TX_SIG_LENGTH;

public class Transaction {

    private static final Logger log = LoggerFactory.getLogger(Transaction.class);

    private static final int SIGNATURE_LENGTH = 65;

    // Transaction Data Format v0.0.3
    private final TransactionHeader header;
    private final byte[] signature;
    private final TransactionBody body;

    private byte[] binary;

    /**
     * Transaction Constructor.
     *
     * @param header transaction header
     * @param signature transaction signature
     * @param body   transaction body
     */
    public Transaction(TransactionHeader header, byte[] signature, TransactionBody body) {
        this.header = header;
        this.signature = signature;
        this.body = body;
    }

    /**
     * Transaction Constructor.
     *
     * @param header transaction header
     * @param wallet wallet for signing
     * @param body   transaction body
     */
    public Transaction(TransactionHeader header, Wallet wallet, TransactionBody body) {
        this(header, wallet.sign(header.getHashForSigning(), true), body);
    }

    /**
     * Transaction Constructor.
     *
     * @param jsonObject jsonObject transaction.
     */
    public Transaction(JsonObject jsonObject) {
        this(new TransactionHeader(jsonObject.getAsJsonObject(HEADER)),
                Hex.decode(jsonObject.get(SIGNATURE).getAsString()),
                new TransactionBody(jsonObject.getAsJsonArray(BODY)));
    }

    /**
     * Transaction Constructor.
     *
     * @param txBytes binary transaction.
     */
    public Transaction(byte[] txBytes) {
        int position = 0;

        byte[] headerBytes = new byte[TX_HEADER_LENGTH];
        System.arraycopy(txBytes, 0, headerBytes, 0, headerBytes.length);
        this.header = new TransactionHeader(headerBytes);
        position += headerBytes.length;

        byte[] sigBytes = new byte[TX_SIG_LENGTH];
        System.arraycopy(txBytes, position, sigBytes, 0, sigBytes.length);
        position += sigBytes.length;
        this.signature = sigBytes;

        long bodyLength = this.header.getBodyLength();
        if (bodyLength > TX_BODY_MAX_LENGTH) {
            throw new NotValidateException();
        }

        byte[] bodyBytes = new byte[(int)bodyLength];
        System.arraycopy(txBytes, position, bodyBytes, 0, bodyBytes.length);
        position += bodyBytes.length;
        this.body = new TransactionBody(bodyBytes);

        if (position != txBytes.length) {
            throw new NotValidateException();
        }
    }

    /**
     * Get TransactionHeader.
     *
     * @return transaction header class
     */
    public TransactionHeader getHeader() {
        return this.header;
    }

    /**
     * Get TransactionBody.
     *
     * @return transaction body class
     */
    public TransactionBody getBody() {
        return this.body;
    }

    /**
     * Get Transaction signature.
     *
     * @return transaction signature
     */
    public byte[] getSignature() {
        return signature;
    }

    /**
     * Get transaction hash. SHA3(header | signature)
     *
     * @return transaction hash
     */
    public byte[] getHash() {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        try {
            bao.write(this.header.toBinary());
            bao.write(this.signature);
        } catch (IOException e) {
            log.warn(e.getMessage());
            return new byte[0];
        }
        return HashUtil.sha3(bao.toByteArray());
    }

    /**
     * Get transaction hash(HexString).
     *
     * @return transaction hash(HexString)
     */
    String getHashString() {
        return Hex.toHexString(this.getHash());
    }

    /**
     * Get the public key.
     *
     * @return public key
     */
    byte[] getPubKey() throws SignatureException {
        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(this.signature);
        ECKey ecKeyPub = ECKey.signatureToKey(this.header.getHashForSigning(), ecdsaSignature);

        return ecKeyPub.getPubKey();
    }

    /**
     * Get the public key as HexString.
     *
     * @return the public key as HexString
     */
    String getPubKeyHexString() throws SignatureException {
        return Hex.toHexString(this.getPubKey());
    }

    /**
     * Get the address as binary.
     *
     * @return address
     */
    public byte[] getAddress() throws SignatureException {

        byte[] pubKey = this.getPubKey();
        return HashUtil.sha3omit12(
                Arrays.copyOfRange(pubKey, 1, pubKey.length));
    }

    /**
     * Get the address as HexString.
     *
     * @return address as HexString
     */
    String getAddressToString() throws SignatureException {
        return Hex.toHexString(this.getAddress());
    }

    /**
     * Get the Transaction length (Header + Signature + Body).
     *
     * @return tx length
     */
    public long length() {
        return Constants.TX_HEADER_LENGTH + this.signature.length + this.body.length();
    }

    /**
     * Verify a transaction.(data format & signing)
     *
     * @return true(success), false(fail)
     */
    public boolean verify() {

        if (!this.verifyData()) {
            return false;
        }

        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(this.signature);
        byte[] hashedHeader = this.header.getHashForSigning();
        ECKey ecKeyPub;

        try {
            ecKeyPub = ECKey.signatureToKey(hashedHeader, ecdsaSignature);

        } catch (SignatureException e) {
            throw new InvalidSignatureException(e);
        }

        return ecKeyPub.verify(hashedHeader, ecdsaSignature);

    }

    private boolean verifyCheckLengthNotNull(byte[] data, int length, String msg) {

        boolean result = !(data == null || data.length != length);

        if (!result) {
            log.debug("{} is not valid.", msg);
        }

        return result;
    }

    /**
     * Verify a transaction about transaction format.
     *
     * @return true(success), false(fail)
     */
    private boolean verifyData() {
        // todo: error code
        boolean check = true;

        check &= verifyCheckLengthNotNull(
                this.header.getChain(), TransactionHeader.CHAIN_LENGTH, "chain");
        check &= verifyCheckLengthNotNull(
                this.header.getVersion(), TransactionHeader.VERSION_LENGTH, "version");
        check &= verifyCheckLengthNotNull(
                this.header.getType(), TransactionHeader.TYPE_LENGTH, "type");
        check &= this.header.getTimestamp() > TIMESTAMP_2018;
        check &= verifyCheckLengthNotNull(
                this.header.getBodyHash(), TransactionHeader.BODYHASH_LENGTH, "bodyHash");
        check &= !(this.header.getBodyLength() <= 0
                || this.header.getBodyLength() != this.getBody().length());
        check &= verifyCheckLengthNotNull(this.signature, SIGNATURE_LENGTH, SIGNATURE);

        // check bodyHash
        if (!Arrays.equals(this.header.getBodyHash(), HashUtil.sha3(body.toBinary()))) {
            String bodyHash = Hex.toHexString(header.getBodyHash());
            log.debug("bodyHash is not equal to body :{}", bodyHash);
            return false;
        }

        return check;
    }

    /**
     * Convert from Transaction.class to JsonObject.
     *
     * @return transaction as JsonObject
     */
    public JsonObject toJsonObject() {

        JsonObject jsonObject = new JsonObject();
        jsonObject.add(HEADER, this.header.toJsonObject());
        jsonObject.addProperty(SIGNATURE, Hex.toHexString(this.signature));
        jsonObject.add(BODY, this.body.getBody());

        return jsonObject;
    }

    /**
     * Print transaction.
     */
    public String toString() {
        return this.toJsonObject().toString();
    }

    /**
     * Print transaction to pretty JsonObject.
     */
    String toStringPretty() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this.toJsonObject());
    }

    /**
     * Get a binary transaction data.
     *
     * @return a binary transaction data.
     */
    public byte[] toBinary() {
        if (binary != null) {
            return binary;
        }
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        try {
            bao.write(this.header.toBinary());
            bao.write(this.signature);
            bao.write(this.body.toBinary());

            binary = bao.toByteArray();
            return binary;
        } catch (IOException e) {
            log.warn("Transaction toBinary() IOException");
            throw new NotValidateException();
        }
    }

    public static Proto.Transaction toProtoTransaction(Transaction tx) {
        // todo: move at TransactionHusk

        Proto.Transaction.Header protoHeader;
        protoHeader = Proto.Transaction.Header.newBuilder()
            .setChain(ByteString.copyFrom(tx.getHeader().getChain()))
            .setVersion(ByteString.copyFrom(tx.getHeader().getVersion()))
            .setType(ByteString.copyFrom(tx.getHeader().getType()))
            .setTimestamp(Timestamps.fromMillis(tx.getHeader().getTimestamp()))
            .setBodyHash(ByteString.copyFrom(tx.getHeader().getBodyHash()))
            .setBodyLength(tx.getHeader().getBodyLength())
            .build();

        return Proto.Transaction.newBuilder()
                .setHeader(protoHeader)
                .setSignature(ByteString.copyFrom(tx.getSignature()))
                .setBody(ByteString.copyFrom(tx.getBody().toBinary()))
                .build();
    }

    static Transaction toTransaction(Proto.Transaction protoTransaction) {
        // todo: move at TransactionHusk

        TransactionHeader txHeader = new TransactionHeader(
                protoTransaction.getHeader().getChain().toByteArray(),
                protoTransaction.getHeader().getVersion().toByteArray(),
                protoTransaction.getHeader().getType().toByteArray(),
                Timestamps.toMillis(protoTransaction.getHeader().getTimestamp()),
                protoTransaction.getHeader().getBodyHash().toByteArray(),
                protoTransaction.getHeader().getBodyLength()
                );

        TransactionBody txBody = new TransactionBody(
                protoTransaction.getBody().toStringUtf8()
        );

        return new Transaction(txHeader, protoTransaction.getSignature().toByteArray(), txBody);

    }
}
