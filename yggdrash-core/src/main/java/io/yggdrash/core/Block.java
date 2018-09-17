package io.yggdrash.core;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.core.genesis.BlockInfo;
import io.yggdrash.core.genesis.TransactionInfo;
import io.yggdrash.crypto.ECKey;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.Proto;
import io.yggdrash.trie.Trie;
import io.yggdrash.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Block implements Cloneable {

    private static final Logger log = LoggerFactory.getLogger(Block.class);

    private static final int SIGNATURE_LENGTH = 65;

    private BlockHeader header;
    private byte[] signature;
    private BlockBody body;

    public Block(BlockHeader header, byte[] signature, BlockBody body) {
        this.header = header;
        this.signature = signature;
        this.body = body;
    }

    public Block(BlockHeader header, Wallet wallet, BlockBody body)
            throws IOException {
        this.header = header;
        this.body = body;
        this.signature = wallet.signHashedData(this.header.getHashForSignning());
    }

    public Block(JsonObject jsonObject) {
        this.header = new BlockHeader(jsonObject.get("header").getAsJsonObject());
        this.signature = Hex.decode(jsonObject.get("signature").getAsString());
        this.body = new BlockBody(jsonObject.getAsJsonArray("body"));
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

    public byte[] getHash() throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        bao.write(this.header.toBinary());
        bao.write(this.signature);

        return HashUtil.sha3(bao.toByteArray());
    }

    public String getHashHexString() throws IOException {
        return org.spongycastle.util.encoders.Hex.toHexString(this.getHash());
    }

    public byte[] getPubKey() throws IOException, SignatureException {
        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(this.signature);
        ECKey ecKeyPub = ECKey.signatureToKey(this.header.getHashForSignning(), ecdsaSignature);

        return ecKeyPub.getPubKey();
    }

    public String getPubKeyHexString() throws IOException, SignatureException {
        return Hex.toHexString(this.getPubKey());
    }

    public byte[] getAddress() throws IOException, SignatureException {
        byte[] pubBytes = this.getPubKey();
        return HashUtil.sha3omit12(
                Arrays.copyOfRange(pubBytes, 1, pubBytes.length));
    }

    public String getAddressHexString() throws IOException, SignatureException {
        return Hex.toHexString(getAddress());
    }

    public long length() throws IOException {
        return this.header.length() + this.signature.length + this.body.length();
    }

    public boolean verify() throws IOException, SignatureException {

        if (!this.verifyData()) {
            return false;
        }

        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(this.signature);
        byte[] hashedHeader = this.header.getHashForSignning();
        ECKey ecKeyPub = ECKey.signatureToKey(hashedHeader, ecdsaSignature);

        return ecKeyPub.verify(hashedHeader, ecdsaSignature);
    }


    /**
     * Verify a block about block format.
     *
     * @return true(success), false(fail)
     */
    public boolean verifyData() throws IOException {

        // check header size & null
        if (this.header.getChain() == null
                || this.header.getChain().length != this.header.CHAIN_LENGTH) {
            log.debug("chain is not valid.");
            return false;
        } else if (this.header.getVersion() == null
                || this.header.getVersion().length != this.header.VERSION_LENGTH) {
            log.debug("version is not valid.");
            return false;
        } else if (this.header.getType() == null
                || this.header.getType().length != this.header.TYPE_LENGTH) {
            log.debug("type is not valid.");
            return false;
        } else if (this.header.getPrevBlockHash() == null
                || this.header.getPrevBlockHash().length != this.header.PREVBLOCKHASH_LENGTH) {
            log.debug("type is not valid.");
            return false;
        } else if (this.header.getIndex() < 0) {
            log.debug("index is not valid. " + this.header.getIndex());
            return false;
        } else if (this.header.getTimestamp() <= 0) {
            log.debug("timestamp is not valid. " + this.header.getTimestamp());
            return false;
        } else if (this.header.getMerkleRoot() == null
                || this.header.getMerkleRoot().length != this.header.MERKLEROOT_LENGTH) {
            log.debug("merkleRoot is not valid.");
            return false;
        } else if (this.header.getBodyLength() <= 0
                || this.header.getBodyLength() != this.getBody().length()) {
            log.debug("bodyLength is not valid. " + this.header.getBodyLength());
            return false;
        } else if (this.signature == null || this.signature.length != SIGNATURE_LENGTH) {
            log.debug("signature is not valid.");
            return false;
        }

        // check merkleRoot
        if (!Arrays.equals(this.header.getMerkleRoot(), Trie.getMerkleRoot(this.body.getBody()))) {
            log.debug("merkleRoot is not equal to body :"
                    + Hex.toHexString(this.header.getMerkleRoot()));
            return false;
        }

        return true;
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

    public byte[] toBinary() throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        bao.write(this.header.toBinary());
        bao.write(this.signature);
        bao.write(this.body.toBinary());

        return bao.toByteArray();
    }

    @Override
    public Block clone() throws CloneNotSupportedException {
        Block block = (Block) super.clone();
        block.header = this.header.clone();
        block.signature = this.signature.clone();
        block.body = this.body.clone();

        return block;
    }

    public Proto.Block toProtoBlock() {
        return this.toProtoBlock(this);
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

        BlockSignature blockSignature
                =  new BlockSignature(protoBlock.getSignature().toByteArray());

        List<Transaction> txList = new ArrayList<>();

        for (Proto.Transaction tx : protoBlock.getBody().getTransactionsList()) {
            txList.add(Transaction.toTransaction(tx));
        }

        BlockBody txBody = new BlockBody(txList);

        return new Block(blockHeader, blockSignature.getSignature(), txBody);

    }

    public static Block fromBlockInfo(BlockInfo blockinfo) {
        BlockHeader blockHeader = new BlockHeader(
                Hex.decode(blockinfo.header.chain),
                Hex.decode(blockinfo.header.version),
                Hex.decode(blockinfo.header.type),
                Hex.decode(blockinfo.header.prevBlockHash),
                ByteUtil.byteArrayToLong(Hex.decode(blockinfo.header.index)),
                ByteUtil.byteArrayToLong(Hex.decode(blockinfo.header.timestamp)),
                Hex.decode(blockinfo.header.merkleRoot),
                ByteUtil.byteArrayToLong(Hex.decode(blockinfo.header.bodyLength))
        );

        List<Transaction> txList = new ArrayList<>();

        for (TransactionInfo txi : blockinfo.body) {
            txList.add(Transaction.fromTransactionInfo(txi));
        }

        BlockBody txBody = new BlockBody(txList);

        return new Block(blockHeader, Hex.decode(blockinfo.signature), txBody);
    }

}
