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

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.yggdrash.common.RawTransaction;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Address;
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

public class TransactionImpl implements Transaction {
    private static final Logger log = LoggerFactory.getLogger(TransactionImpl.class);

    private final Proto.Transaction protoTransaction;

    private final transient TransactionHeader header;
    private final transient TransactionBody body;
    private transient Sha3Hash hash;
    private transient Address address;

    /**
     * Transaction Constructor.
     *
     * @param bytes binary transaction
     */
    public TransactionImpl(byte[] bytes) {
        this(toProto(bytes));
    }

    public TransactionImpl(Proto.Transaction protoTransaction) {
        this.protoTransaction = protoTransaction;
        this.header = new TransactionHeader(protoTransaction.getHeader());
        this.body = new TransactionBody(protoTransaction.getBody());
    }

    /**
     * Transaction Constructor.
     *
     * @param header transaction header
     * @param wallet wallet for signing
     * @param body   transaction body
     */
    public TransactionImpl(TransactionHeader header, Wallet wallet, TransactionBody body) {
        this(header, wallet.sign(header.getHashForSigning(), true), body);
    }

    /**
     * Transaction Constructor.
     *
     * @param header    transaction header
     * @param signature transaction signature
     * @param body      transaction body
     */
    public TransactionImpl(TransactionHeader header, byte[] signature, TransactionBody body) {
        this.header = header;
        this.body = body;
        this.protoTransaction = Proto.Transaction.newBuilder()
                .setHeader(header.getInstance())
                .setSignature(ByteString.copyFrom(signature))
                .setBody(body.toString())
                .build();
    }

    /**
     * Transaction Constructor.
     *
     * @param jsonObject jsonObject transaction
     */
    public TransactionImpl(JsonObject jsonObject) {
        this(new TransactionHeader(jsonObject.getAsJsonObject(HEADER)),
                Hex.decode(jsonObject.get(SIGNATURE).getAsString()),
                new TransactionBody(jsonObject.getAsJsonObject(BODY)));
    }

    @Override
    public TransactionHeader getHeader() {
        return header;
    }

    @Override
    public byte[] getSignature() {
        return protoTransaction.getSignature().toByteArray();
    }

    @Override
    public TransactionBody getBody() {
        return body;
    }

    @Override
    public BranchId getBranchId() {
        return BranchId.of(header.getChain());
    }

    @Override
    public Sha3Hash getHash() {
        if (hash == null) {
            setHash();
        }
        return hash;
    }

    private void setHash() {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        try {
            bao.write(header.getBinaryForSigning());
            bao.write(getSignature());
        } catch (IOException e) {
            throw new NotValidateException();
        }
        this.hash = new Sha3Hash(bao.toByteArray());
    }

    @Override
    public byte[] getPubKey() {
        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(getSignature());
        try {
            ECKey ecKeyPub = ECKey.signatureToKey(this.header.getHashForSigning(), ecdsaSignature);
            return ecKeyPub.getPubKey();
        } catch (SignatureException e) {
            throw new InvalidSignatureException(e);
        }
    }

    @Override
    public Address getAddress() {
        if (address == null) {
            setAddress();
        }
        return address;
    }

    private void setAddress() {
        try {
            this.address = new Address(getPubKey());
        } catch (Exception e) {
            this.address = Address.NULL_ADDRESS;
        }
    }

    @Override
    public long getLength() {
        return TransactionHeader.LENGTH + Constants.SIGNATURE_LENGTH + header.getBodyLength();
    }

    @Override
    public byte[] toBinary() {
        return protoTransaction.toByteArray();
    }

    @Override
    public Proto.Transaction getInstance() {
        return protoTransaction;
    }

    @Override
    public boolean verify() {

        if (!this.verifyData()) {
            return false;
        }

        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(getSignature());
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
        // TODO CheckByValidate By Code
        boolean check = true;

        check &= verifyCheckLengthNotNull(
                this.header.getChain(), Constants.BRANCH_LENGTH, "chain");
        check &= verifyCheckLengthNotNull(
                this.header.getVersion(), TransactionHeader.VERSION_LENGTH, "version");
        check &= verifyCheckLengthNotNull(
                this.header.getType(), TransactionHeader.TYPE_LENGTH, "type");
        check &= this.header.getTimestamp() > TIMESTAMP_2018;
        check &= verifyCheckLengthNotNull(
                this.header.getBodyHash(), Constants.HASH_LENGTH, "bodyHash");
        check &= !(this.header.getBodyLength() <= 0
                || this.header.getBodyLength() != this.getBody().getLength());
        check &= verifyCheckLengthNotNull(getSignature(), Constants.SIGNATURE_LENGTH, SIGNATURE);

        // check bodyHash
        if (!Arrays.equals(this.header.getBodyHash(), HashUtil.sha3(body.toBinary()))) {
            String bodyHash = Hex.toHexString(header.getBodyHash());
            log.debug("bodyHash is not equal to body :{}", bodyHash);
            return false;
        }

        return check;
    }

    @Override
    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.add(HEADER, this.header.toJsonObject());
        jsonObject.addProperty(SIGNATURE, Hex.toHexString(getSignature()));
        jsonObject.add(BODY, this.body.getBody());

        return jsonObject;
    }

    @Override
    public JsonObject toJsonObjectFromProto() {
        try {
            String print = JsonFormat.printer()
                    .includingDefaultValueFields().print(this.protoTransaction);
            JsonObject asJsonObject = new JsonParser().parse(print).getAsJsonObject();
            asJsonObject.addProperty("txId", getHash().toString());
            return asJsonObject;
        } catch (InvalidProtocolBufferException e) {
            log.warn(e.getMessage());
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransactionImpl other = (TransactionImpl) o;
        return Arrays.equals(toBinary(), other.toBinary());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(toBinary());
    }

    @Override
    public String toString() {
        return toJsonObject().toString();
    }

    @Override
    public int compareTo(Transaction o) {
        return Long.compare(getHeader().getTimestamp(), o.getHeader().getTimestamp());
    }

    @VisibleForTesting
    public byte[] toRawTransaction() {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        try {
            bao.write(header.getBinaryForSigning());
            bao.write(getSignature());
            bao.write(body.toBinary());
        } catch (IOException e) {
            throw new NotValidateException();
        }
        return bao.toByteArray();
    }

    public static Transaction parseFromRaw(byte[] bytes) {
        RawTransaction raw = new RawTransaction(bytes);

        TransactionHeader header = new TransactionHeader(
                raw.getChain(),
                raw.getVersion(),
                raw.getType(),
                raw.getTimestamp(),
                raw.getBodyHash(),
                raw.getBodyLength());

        TransactionBody body = new TransactionBody(raw.getBody());

        return new TransactionImpl(header, raw.getSignature(), body);
    }

    private static Proto.Transaction toProto(byte[] bytes) {
        try {
            return Proto.Transaction.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
        }
    }
}
