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
import io.yggdrash.common.trie.Trie;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static io.yggdrash.common.config.Constants.KEY.BODY;
import static io.yggdrash.common.config.Constants.KEY.HEADER;
import static io.yggdrash.common.config.Constants.KEY.SIGNATURE;
import static io.yggdrash.common.config.Constants.TIMESTAMP_2018;

public class Block {

    private static final Logger log = LoggerFactory.getLogger(Block.class);

    private static final int HEADER_LENGTH = 124;
    private static final int SIGNATURE_LENGTH = 65;

    private BlockHeader header;
    private byte[] signature;
    private BlockBody body;

    private byte[] binary;

    public Block(BlockHeader header, byte[] signature, BlockBody body) {
        this.header = header;
        this.signature = signature;
        this.body = body;
    }

    public Block(BlockHeader header, Wallet wallet, BlockBody body) {
        this(header, wallet.sign(header.getHashForSigning(), true), body);
    }

    public Block(JsonObject jsonObject) {
        this(new BlockHeader(jsonObject.getAsJsonObject(HEADER)),
                Hex.decode(jsonObject.get(SIGNATURE).getAsString()),
                new BlockBody(jsonObject.getAsJsonArray(BODY)));
    }

    public Block(byte[] blockBytes) {
        int position = 0;

        byte[] headerBytes = new byte[HEADER_LENGTH];
        System.arraycopy(blockBytes, 0, headerBytes, 0, headerBytes.length);
        this.header = new BlockHeader(headerBytes);
        position += headerBytes.length;

        byte[] sigBytes = new byte[SIGNATURE_LENGTH];
        System.arraycopy(blockBytes, position, sigBytes, 0, sigBytes.length);
        position += sigBytes.length;
        this.signature = sigBytes;

        long bodyLength = this.header.getBodyLength();
        if (bodyLength < 0 || bodyLength > Constants.MAX_MEMORY) {
            log.debug("Block body length is not valid");
            throw new NotValidateException();
        }
        byte[] bodyBytes = new byte[(int)bodyLength];
        System.arraycopy(blockBytes, position, bodyBytes, 0, bodyBytes.length);
        position += bodyBytes.length;
        this.body = new BlockBody(bodyBytes);

        if (position != blockBytes.length) {
            throw new NotValidateException();
        }
    }

    public BlockHeader getHeader() {
        return header;
    }

    public byte[] getSignature() {
        return signature;
    }

    public BlockBody getBody() {
        return body;
    }

    public long getIndex() {
        return this.header.getIndex();
    }

    public byte[] getChain() {
        return this.header.getChain();
    }

    public String getChainHex() {
        return Hex.toHexString(this.header.getChain());
    }

    public byte[] getHash() {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        try {
            bao.write(this.header.toBinary());
            bao.write(this.signature);
        } catch (IOException e) {
            log.warn("getHash() ioException");
            throw new NotValidateException();
        }

        return HashUtil.sha3(bao.toByteArray());
    }

    public String getHashHex() {
        return org.spongycastle.util.encoders.Hex.toHexString(this.getHash());
    }

    public byte[] getPrevBlockHash() {
        return this.header.getPrevBlockHash();
    }

    public byte[] getPubKey() {
        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(this.signature);
        ECKey ecKeyPub;
        try {
            ecKeyPub = ECKey.signatureToKey(this.header.getHashForSigning(), ecdsaSignature);
        } catch (SignatureException e) {
            log.warn(e.getMessage());
            throw new InvalidSignatureException();
        }

        return ecKeyPub.getPubKey();
    }

    String getPubKeyHex() {
        return Hex.toHexString(this.getPubKey());
    }

    public byte[] getAddress() {
        byte[] pubBytes = this.getPubKey();
        return HashUtil.sha3omit12(
                Arrays.copyOfRange(pubBytes, 1, pubBytes.length));
    }

    public String getAddressHex() {
        return Hex.toHexString(getAddress());
    }

    public long length() {
        return this.header.length() + this.signature.length + this.body.length();
    }

    public boolean verify() {
        // Block DAta Verify
        if (!this.verifyData()) {
            return false;
        }

        if (this.header.getIndex() == 0) { // Genesis
            // TODO Genesis Block Check
            return true;
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
     * Verify a block about block format.
     *
     * @return true(success), false(fail)
     */
    private boolean verifyData() {
        // TODO CheckByValidate By Code
        boolean check = true;
        check &= verifyCheckLengthNotNull(
                this.header.getChain(), BlockHeader.CHAIN_LENGTH, "chain");
        check &= verifyCheckLengthNotNull(
                this.header.getVersion(), BlockHeader.VERSION_LENGTH, "version");
        check &= verifyCheckLengthNotNull(header.getType(), BlockHeader.TYPE_LENGTH, "type");
        check &= verifyCheckLengthNotNull(
                this.header.getPrevBlockHash(), BlockHeader.PREVBLOCKHASH_LENGTH, "prevBlockHash");
        check &= verifyCheckLengthNotNull(
                this.header.getMerkleRoot(), BlockHeader.MERKLEROOT_LENGTH, "merkleRootLength");
        if (header.getIndex() != 0) {
            // Genesis Block is not check signature
            check &= verifyCheckLengthNotNull(this.signature, SIGNATURE_LENGTH, SIGNATURE);
        }
        check &= this.header.getIndex() >= 0;
        check &= this.header.getTimestamp() > TIMESTAMP_2018;
        check &= !(this.header.getBodyLength() < 0
                || this.header.getBodyLength() != this.getBody().length());
        check &= Arrays.equals(this.header.getMerkleRoot(), Trie.getMerkleRoot(this.body.getBody()));

        return check;
    }

    public JsonObject toJsonObject() {
        if (this.header == null || this.signature == null || this.body == null) {
            return null;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.add(HEADER, this.header.toJsonObject());
        jsonObject.addProperty(SIGNATURE, Hex.toHexString(this.signature));
        jsonObject.add(BODY, this.body.toJsonArray());
        return jsonObject;
    }

    public String toString() {
        return this.toJsonObject().toString();
    }

    public String toStringPretty() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this.toJsonObject());
    }

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
            log.warn("Block toBinary() IOException");
            throw new NotValidateException();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Block other = (Block) o;
        return this.getHeader().equals(other.getHeader())
                && Arrays.equals(this.signature, other.getSignature())
                && this.getBody().equals(other.getBody());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(toBinary());
    }

    @Deprecated
    public Proto.Block toProtoBlock() {
        return toProtoBlock(this);
    }

    public static Proto.Block toProtoBlock(Block block) {
        if (block == null || block.getHeader() == null) {
            return null;
        }

        Proto.Block.Header protoHeader;
        protoHeader = Proto.Block.Header.newBuilder()
                .setChain(ByteString.copyFrom(block.getHeader().getChain()))
                .setVersion(ByteString.copyFrom(block.getHeader().getVersion()))
                .setType(ByteString.copyFrom(block.getHeader().getType()))
                .setPrevBlockHash(ByteString.copyFrom(block.getHeader().getPrevBlockHash()))
                .setIndex(block.getHeader().getIndex())
                .setTimestamp(fromMillis(block.getHeader().getTimestamp()))
                .setMerkleRoot(ByteString.copyFrom(block.getHeader().getMerkleRoot()))
                .setBodyLength(block.getHeader().getBodyLength())
                .build();

        Proto.TransactionList.Builder builder = Proto.TransactionList.newBuilder();
        for (Transaction tx : block.getBody().getBody()) {
            builder.addTransactions(Transaction.toProtoTransaction(tx));
        }

        return Proto.Block.newBuilder()
                .setHeader(protoHeader)
                .setSignature(ByteString.copyFrom(block.getSignature()))
                .setBody(builder.build())
                .build();
    }

    public static Block toBlock(Proto.Block protoBlock) {
        if (protoBlock == null || protoBlock.getSerializedSize() == 0) {
            return null;
        }

        BlockHeader blockHeader = new BlockHeader(
                protoBlock.getHeader().getChain().toByteArray(),
                protoBlock.getHeader().getVersion().toByteArray(),
                protoBlock.getHeader().getType().toByteArray(),
                protoBlock.getHeader().getPrevBlockHash().toByteArray(),
                protoBlock.getHeader().getIndex(),
                Timestamps.toMillis(protoBlock.getHeader().getTimestamp()),
                protoBlock.getHeader().getMerkleRoot().toByteArray(),
                protoBlock.getHeader().getBodyLength()
        );

        List<Transaction> txList = new ArrayList<>();

        for (Proto.Transaction tx : protoBlock.getBody().getTransactionsList()) {
            txList.add(Transaction.toTransaction(tx));
        }

        BlockBody txBody = new BlockBody(txList);

        return new Block(blockHeader, protoBlock.getSignature().toByteArray(), txBody);
    }

    public static long getBlockLengthInBytes(byte[] bytes) {
        if (bytes == null || bytes.length <= HEADER_LENGTH + SIGNATURE_LENGTH) {
            log.debug("Input bytes is not valid");
            return 0L;
        }

        byte[] headerBytes = new byte[HEADER_LENGTH];
        System.arraycopy(bytes, 0, headerBytes, 0, headerBytes.length);
        BlockHeader header = new BlockHeader(headerBytes);
        long bodyLength = header.getBodyLength();

        return (long) HEADER_LENGTH + (long) SIGNATURE_LENGTH + bodyLength;
    }

    public void clear() {
        this.header = null;
        if (this.body != null) {
            this.body.clear();
        }
        this.body = null;
        this.signature = null;
    }
}
