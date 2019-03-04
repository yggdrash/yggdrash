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
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.trie.Trie;
import io.yggdrash.common.utils.ByteUtil;
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

import static io.yggdrash.common.config.Constants.EMPTY_BYTE32;
import static io.yggdrash.common.config.Constants.TIMESTAMP_2018;

public class Block {

    private static final Logger log = LoggerFactory.getLogger(Block.class);

    private static final int HEADER_LENGTH = 124;
    private static final int SIGNATURE_LENGTH = 65;

    private BlockHeader header;
    private byte[] signature;
    private BlockBody body;

    public Block(BlockHeader header, byte[] signature, BlockBody body) {
        this.header = header;
        this.signature = signature;
        this.body = body;
    }

    public Block(BlockHeader header, Wallet wallet, BlockBody body) {
        this(header, wallet.signHashedData(header.getHashForSigning()), body);
    }

    public Block(JsonObject jsonObject) {
        this(new BlockHeader(jsonObject.getAsJsonObject("header")),
                Hex.decode(jsonObject.get("signature").getAsString()),
                new BlockBody(jsonObject.getAsJsonArray("body")));
    }

    public Block(byte[] blockBytes) {
        int position = 0;

        byte[] headerBytes = new byte[HEADER_LENGTH];
        System.arraycopy(blockBytes, 0, headerBytes, 0, headerBytes.length);
        this.header = new BlockHeader(headerBytes);
        position += headerBytes.length;
        headerBytes = null;

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

        bodyBytes = null;

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
        ECKey ecKeyPub = null;
        try {
            ecKeyPub = ECKey.signatureToKey(this.header.getHashForSigning(), ecdsaSignature);
        } catch (SignatureException e) {
            log.warn(e.getMessage());
            throw new InvalidSignatureException();
        }

        return ecKeyPub.getPubKey();
    }

    public String getPubKeyHexString() {
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
            log.debug(msg + " is not valid.");
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
            check &= verifyCheckLengthNotNull(this.signature, SIGNATURE_LENGTH, "signature");
        }
        check &= this.header.getIndex() >= 0;
        check &= this.header.getTimestamp() > TIMESTAMP_2018;
        check &= !(this.header.getBodyLength() < 0
                || this.header.getBodyLength() != this.getBody().length());
        check &= Arrays.equals(
                Arrays.equals(this.header.getMerkleRoot(),
                        EMPTY_BYTE32) ? null : this.header.getMerkleRoot(),
                Trie.getMerkleRoot(this.body.getBody()));

        return check;
    }

    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("header", this.header.toJsonObject());
        jsonObject.addProperty("signature", Hex.toHexString(this.signature));
        jsonObject.add("body", this.body.toJsonArray());
        return jsonObject;
    }

    public String toString() {
        return this.toJsonObject().toString();
    }

    public String toStringPretty() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this.toJsonObject());
    }

    public byte[] toBinary() {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        try {
            bao.write(this.header.toBinary());
            bao.write(this.signature);
            bao.write(this.body.toBinary());
        } catch (IOException e) {
            log.warn("Block toBinary() IOException");
            throw new NotValidateException();
        }

        return bao.toByteArray();
    }

    public Block clone() {
        return new Block(this.header.clone(), this.signature.clone(), this.body.clone());
    }

    public boolean equals(Block newBlock) {
        return this.getHeader().equals(newBlock.getHeader())
                && Arrays.equals(this.signature, newBlock.getSignature())
                && this.getBody().equals(newBlock.getBody());
    }

    public Proto.Block toProtoBlock() {
        return toProtoBlock(this);
    }

    public static Proto.Block toProtoBlock(Block block) {
        Proto.Block.Header protoHeader;
        protoHeader = Proto.Block.Header.newBuilder()
            .setChain(ByteString.copyFrom(block.getHeader().getChain()))
            .setVersion(ByteString.copyFrom(block.getHeader().getVersion()))
            .setType(ByteString.copyFrom(block.getHeader().getType()))
            .setPrevBlockHash(ByteString.copyFrom(block.getHeader().getPrevBlockHash()))
            .setIndex(ByteString.copyFrom(ByteUtil.longToBytes(block.getHeader().getIndex())))
            .setTimestamp(
                    ByteString.copyFrom(ByteUtil.longToBytes(block.getHeader().getTimestamp())))
            .setMerkleRoot(ByteString.copyFrom(block.getHeader().getMerkleRoot()))
            .setBodyLength(
                    ByteString.copyFrom(ByteUtil.longToBytes(block.getHeader().getBodyLength())))
            .build();

        Proto.TransactionList.Builder builder = Proto.TransactionList.newBuilder();
        for (Transaction tx : block.getBody().getBody()) {
            builder.addTransactions(Transaction.toProtoTransaction(tx));
        }

        Proto.Block protoBlock = Proto.Block.newBuilder()
                .setHeader(protoHeader)
                .setSignature(ByteString.copyFrom(block.getSignature()))
                .setBody(builder.build())
                .build();

        return protoBlock;
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
                ByteUtil.byteArrayToLong(protoBlock.getHeader().getIndex().toByteArray()),
                ByteUtil.byteArrayToLong(protoBlock.getHeader().getTimestamp().toByteArray()),
                protoBlock.getHeader().getMerkleRoot().toByteArray(),
                ByteUtil.byteArrayToLong(protoBlock.getHeader().getBodyLength().toByteArray())
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
        headerBytes = null;
        long bodyLength = header.getBodyLength();

        return (long) HEADER_LENGTH + (long) SIGNATURE_LENGTH + bodyLength;
    }

    public void clear() {
        this.header = null;
        this.body.clear();
        this.body = null;
        this.signature = null;
    }
}
