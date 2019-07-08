/*
 * Copyright 2019 Akashic Foundation
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

package io.yggdrash.common;

import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.common.utils.SerializationUtil;
import org.spongycastle.util.encoders.Hex;

public class RawTransaction  {

    private static final int HEADER_LENGTH = 84;

    private static final int CHAIN_LENGTH = 20;
    private static final int VERSION_LENGTH = 8;
    private static final int TYPE_LENGTH = 8;
    private static final int TIMESTAMP_LENGTH = 8;
    private static final int BODY_HASH_LENGTH = 32;
    private static final int BODY_LENGTH_LENGTH = 8;

    private static final int SIGNATURE_LENGTH = 65;

    // Transaction Format v0.0.3
    private final byte[] chain;       // 20 Bytes
    private final byte[] version;     // 8 Bytes
    private final byte[] type;        // 8 Bytes
    private final long timestamp;     // 8 Bytes
    private final byte[] bodyHash;    // 32 Bytes
    private final long bodyLength;    // 8 Bytes

    private byte[] signature;         // 65 Bytes

    private final String body;        // Json string

    public RawTransaction(byte[] bytes) {

        int pos = 0;

        // Header parse
        this.chain = new byte[CHAIN_LENGTH];
        System.arraycopy(bytes, pos, this.chain, 0, this.chain.length);
        pos += this.chain.length;

        this.version = new byte[VERSION_LENGTH];
        System.arraycopy(bytes, pos, this.version, 0, this.version.length);
        pos += this.version.length;

        this.type = new byte[TYPE_LENGTH];
        System.arraycopy(bytes, pos, this.type, 0, this.type.length);
        pos += this.type.length;

        byte[] timestampBytes = new byte[TIMESTAMP_LENGTH];
        System.arraycopy(bytes, pos, timestampBytes, 0, timestampBytes.length);
        this.timestamp = ByteUtil.byteArrayToLong(timestampBytes);
        pos += timestampBytes.length;

        this.bodyHash = new byte[BODY_HASH_LENGTH];
        System.arraycopy(bytes, pos, this.bodyHash, 0, this.bodyHash.length);
        pos += this.bodyHash.length;

        byte[] bodyLengthBytes = new byte[BODY_LENGTH_LENGTH];
        System.arraycopy(bytes, pos, bodyLengthBytes, 0, bodyLengthBytes.length);
        this.bodyLength = ByteUtil.byteArrayToLong(bodyLengthBytes);
        pos += bodyLengthBytes.length;

        long expected = HEADER_LENGTH + SIGNATURE_LENGTH + bodyLength;
        if (this.bodyLength < 0 ||  expected != bytes.length) {
            String format = "Invalid body bytes. HeaderBodyLength=(%d). Expected=(%d), Actual=(%d), InputData=(%s)";
            throw new FailedOperationException(
                    String.format(format, bodyLength, expected, bytes.length, printInputData(bytes)));

        }

        // Signature parse
        byte[] sigBytes = new byte[SIGNATURE_LENGTH];
        System.arraycopy(bytes, pos, sigBytes, 0, sigBytes.length);
        this.signature = sigBytes;
        pos += sigBytes.length;

        // Body parse
        byte[] bodyBytes = new byte[(int)bodyLength];
        System.arraycopy(bytes, pos, bodyBytes, 0, bodyBytes.length);
        this.body = SerializationUtil.deserializeString(bodyBytes);
    }

    public byte[] getChain() {
        return chain;
    }

    public byte[] getVersion() {
        return version;
    }

    public byte[] getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getBodyHash() {
        return bodyHash;
    }

    public long getBodyLength() {
        return bodyLength;
    }

    public byte[] getSignature() {
        return signature;
    }

    public String getBody() {
        return body;
    }

    private String printInputData(byte[] bytes) {
        byte[] headerBytes = new byte[HEADER_LENGTH];
        System.arraycopy(bytes, 0, headerBytes, 0, headerBytes.length);

        byte[] sigBytes = new byte[SIGNATURE_LENGTH];
        System.arraycopy(bytes, HEADER_LENGTH, sigBytes, 0, sigBytes.length);

        byte[] bodyBytes = new byte[(int) bodyLength];
        System.arraycopy(bytes, HEADER_LENGTH + SIGNATURE_LENGTH, bodyBytes, 0, bodyBytes.length);

        return String.format("{header=(%s), signature=(%s), body=(%s)}",
                Hex.toHexString(headerBytes),
                Hex.toHexString(sigBytes),
                SerializationUtil.deserializeString(bodyBytes));
    }

}
