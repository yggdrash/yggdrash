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

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.proto.Proto;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class TransactionHeader implements ProtoObject<Proto.Transaction.Header> {

    public static final int VERSION_LENGTH = 8;
    public static final int TYPE_LENGTH = 8;

    public static final int LENGTH = 84;

    private final Proto.Transaction.Header protoHeader;

    private byte[] binaryForSigning;

    public TransactionHeader(byte[] bytes) {
        this(toProto(bytes));
    }

    public TransactionHeader(Proto.Transaction.Header protoTransactionHeader) {
        this.protoHeader = protoTransactionHeader;
    }

    public TransactionHeader(
            byte[] chain,
            byte[] version,
            byte[] type,
            long timestamp,
            byte[] bodyHash,
            long bodyLength) {

        this.protoHeader = Proto.Transaction.Header.newBuilder()
                .setChain(ByteString.copyFrom(chain))
                .setVersion(ByteString.copyFrom(version))
                .setType(ByteString.copyFrom(type))
                .setTimestamp(timestamp)
                .setBodyHash(ByteString.copyFrom(bodyHash))
                .setBodyLength(bodyLength)
                .build();
    }

    public TransactionHeader(
            byte[] chain,
            byte[] version,
            byte[] type,
            long timestamp,
            TransactionBody txBody) {
        this(chain, version, type, timestamp, txBody.getHash(), txBody.getLength());
    }

    public TransactionHeader(JsonObject jsonObject) {
        this(Hex.decode(jsonObject.get("chain").getAsString()),
                Hex.decode(jsonObject.get("version").getAsString()),
                Hex.decode(jsonObject.get("type").getAsString()),
                HexUtil.hexStringToLong(jsonObject.get("timestamp").getAsString()),
                Hex.decode(jsonObject.get("bodyHash").getAsString()),
                HexUtil.hexStringToLong(jsonObject.get("bodyLength").getAsString()));
    }

    public byte[] getChain() {
        return protoHeader.getChain().toByteArray();
    }

    public byte[] getVersion() {
        return protoHeader.getVersion().toByteArray();
    }

    public byte[] getType() {
        return protoHeader.getType().toByteArray();
    }

    public long getTimestamp() {
        return protoHeader.getTimestamp();
    }

    public byte[] getBodyHash() {
        return protoHeader.getBodyHash().toByteArray();
    }

    public long getBodyLength() {
        return protoHeader.getBodyLength();
    }

    /**
     * Get the headerHash for signing.
     *
     * @return hash of header
     */
    public byte[] getHashForSigning() {
        return HashUtil.sha3(getBinaryForSigning());
    }

    /**
     * Get the binary data of TransactionHeader (84Byte)
     *
     * @return the binary data
     */
    public byte[] getBinaryForSigning() {
        if (binaryForSigning == null) {
            setBinaryForSigning();
        }

        return binaryForSigning;
    }

    private void setBinaryForSigning() {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        try {
            bao.write(getChain());
            bao.write(getVersion());
            bao.write(getType());
            bao.write(ByteUtil.longToBytes(getTimestamp()));
            bao.write(getBodyHash());
            bao.write(ByteUtil.longToBytes(getBodyLength()));
        } catch (IOException e) {
            throw new NotValidateException();
        }

        this.binaryForSigning = bao.toByteArray();
    }

    @Override
    public byte[] toBinary() {
        return protoHeader.toByteArray();
    }

    @Override
    public Proto.Transaction.Header getInstance() {
        return protoHeader;
    }

    /**
     * Convert from TransactionHeader to JsonObject.
     *
     * @return jsonObject of transaction header
     */
    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("chain", Hex.toHexString(getChain()));
        jsonObject.addProperty("version", Hex.toHexString(getVersion()));
        jsonObject.addProperty("type", Hex.toHexString(getType()));
        jsonObject.addProperty("timestamp", Hex.toHexString(ByteUtil.longToBytes(getTimestamp())));
        jsonObject.addProperty("bodyHash", Hex.toHexString(getBodyHash()));
        jsonObject.addProperty("bodyLength", Hex.toHexString(ByteUtil.longToBytes(getBodyLength())));

        return jsonObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BlockHeader other = (BlockHeader) o;
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

    private static Proto.Transaction.Header toProto(byte[] bytes) {
        try {
            return Proto.Transaction.Header.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
        }
    }
}
