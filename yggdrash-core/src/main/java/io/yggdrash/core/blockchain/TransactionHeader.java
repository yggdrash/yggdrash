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
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.core.exception.InternalErrorException;
import io.yggdrash.core.exception.NotValidateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TransactionHeader {

    private static final Logger log = LoggerFactory.getLogger(TransactionHeader.class);

    static final int CHAIN_LENGTH = 20;
    static final int VERSION_LENGTH = 8;
    static final int TYPE_LENGTH = 8;
    static final int TIMESTAMP_LENGTH = 8;
    static final int BODYHASH_LENGTH = 32;
    static final int BODYLENGTH_LENGTH = 8;

    // Transaction Format v0.0.3
    private final byte[] chain;       // 20 Bytes
    private final byte[] version;     // 8 Bytes
    private final byte[] type;        // 8 Bytes
    private final long timestamp;     // 8 Bytes
    private final byte[] bodyHash;    // 32 Bytes
    private final long bodyLength;    // 8 Bytes

    private byte[] binary;

    public TransactionHeader(
            byte[] chain,
            byte[] version,
            byte[] type,
            long timestamp,
            byte[] bodyHash,
            long bodyLength) {
        this.chain = chain;
        this.version = version;
        this.type = type;
        this.timestamp = timestamp;
        this.bodyHash = bodyHash;
        this.bodyLength = bodyLength;
    }

    public TransactionHeader(
            byte[] chain,
            byte[] version,
            byte[] type,
            long timestamp,
            TransactionBody txBody) {
        this(chain, version, type, timestamp, txBody.getBodyHash(), txBody.length());
    }

    public TransactionHeader(JsonObject jsonObject) {
        this.chain = Hex.decode(jsonObject.get("chain").getAsString());
        this.version = Hex.decode(jsonObject.get("version").getAsString());
        this.type = Hex.decode(jsonObject.get("type").getAsString());
        this.timestamp = HexUtil.hexStringToLong(jsonObject.get("timestamp").getAsString());
        this.bodyHash = Hex.decode(jsonObject.get("bodyHash").getAsString());
        this.bodyLength = HexUtil.hexStringToLong(jsonObject.get("bodyLength").getAsString());
    }

    public TransactionHeader(byte[] txHeaderBytes) {
        int pos = 0;

        this.chain = new byte[CHAIN_LENGTH];
        System.arraycopy(txHeaderBytes, pos, this.chain, 0, this.chain.length);
        pos += this.chain.length;

        this.version = new byte[VERSION_LENGTH];
        System.arraycopy(txHeaderBytes, pos, this.version, 0, this.version.length);
        pos += this.version.length;

        this.type = new byte[TYPE_LENGTH];
        System.arraycopy(txHeaderBytes, pos, this.type, 0, this.type.length);
        pos += this.type.length;

        byte[] timestampBytes = new byte[TIMESTAMP_LENGTH];
        System.arraycopy(txHeaderBytes, pos, timestampBytes, 0, timestampBytes.length);
        this.timestamp = ByteUtil.byteArrayToLong(timestampBytes);
        pos += timestampBytes.length;

        this.bodyHash = new byte[BODYHASH_LENGTH];
        System.arraycopy(txHeaderBytes, pos, this.bodyHash, 0, this.bodyHash.length);
        pos += this.bodyHash.length;

        byte[] bodyLengthBytes = new byte[BODYLENGTH_LENGTH];
        System.arraycopy(txHeaderBytes, pos, bodyLengthBytes, 0, bodyLengthBytes.length);
        this.bodyLength = ByteUtil.byteArrayToLong(bodyLengthBytes);
        pos += bodyLengthBytes.length;

        if (pos != txHeaderBytes.length) {
            log.debug("Transaction Header Length is not valid.");
            throw new NotValidateException();
        }
    }


    public byte[] getChain() {
        return this.chain;
    }

    public byte[] getVersion() {
        return this.version;
    }

    public byte[] getType() {
        return this.type;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    byte[] getBodyHash() {
        return this.bodyHash;
    }

    public long getBodyLength() {
        return this.bodyLength;
    }

    /**
     * Get the headerHash for signing.
     *
     * @return hash of header
     */
    public byte[] getHashForSigning() {
        return HashUtil.sha3(this.toBinary());
    }

    /**
     * Get the binary data of TransactionHeader (84Byte)
     *
     * @return the binary data of TransactionHeader (84 byte)
     */
    public byte[] toBinary() {
        if (binary != null) {
            return binary;
        }
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        try {
            bao.write(this.chain);
            bao.write(this.version);
            bao.write(this.type);
            bao.write(ByteUtil.longToBytes(this.timestamp));
            bao.write(this.bodyHash);
            bao.write(ByteUtil.longToBytes(this.bodyLength));

            binary = bao.toByteArray();
            return binary;
        } catch (IOException e) {
            throw new InternalErrorException("toBinary error");
        }
    }

    /**
     * Convert from TransactionHeader to JsonObject.
     *
     * @return jsonObject of transaction header
     */
    public JsonObject toJsonObject() {

        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("chain", Hex.toHexString(this.chain));
        jsonObject.addProperty("version", Hex.toHexString(this.version));
        jsonObject.addProperty("type", Hex.toHexString(this.type));
        jsonObject.addProperty("timestamp", Hex.toHexString(ByteUtil.longToBytes(this.timestamp)));
        jsonObject.addProperty("bodyHash", Hex.toHexString(this.bodyHash));
        jsonObject.addProperty("bodyLength",
                Hex.toHexString(ByteUtil.longToBytes(this.bodyLength)));

        return jsonObject;
    }

    public String toString() {
        return this.toJsonObject().toString();
    }

    public TransactionHeader copy() {
        return new TransactionHeader(this.chain.clone(),
                this.version.clone(),
                this.type.clone(),
                this.timestamp,
                this.bodyHash.clone(),
                this.bodyLength);
    }
}
