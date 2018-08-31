package io.yggdrash.core;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.yggdrash.crypto.ECKey;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.util.TimeUtils;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SignatureException;

public class Block implements Cloneable {

    private BlockHeader header;
    private BlockSignature signature;
    private BlockBody body;

    public Block(BlockHeader header, BlockSignature signature, BlockBody body) {
        this.header = header;
        this.signature = signature;
        this.body = body;
    }

    public Block(BlockHeader header, Wallet wallet, BlockBody body)
            throws IOException, SignatureException {
        this.header = header;
        this.body = body;
        this.header.setTimestamp(TimeUtils.time());
        this.signature = new BlockSignature(wallet, this.header.getHashForSignning());
    }

    public BlockHeader getHeader() {
        return header;
    }

    public BlockSignature getSignature() {
        return signature;
    }

    public BlockBody getBody() {
        return body;
    }

    public JsonObject toJsonObject() {

        JsonObject jsonObject = new JsonObject();

        jsonObject.add("header", this.header.toJsonObject());
        jsonObject.addProperty("signature", this.signature.getSignatureHexString());
        jsonObject.add("body", this.body.toJsonArray());

        return jsonObject;
    }

    public byte[] getHash() throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        bao.write(this.header.toBinary());
        bao.write(this.signature.getSignature());

        return HashUtil.sha3(bao.toByteArray());
    }

    public String getHashString() throws IOException {
        return org.spongycastle.util.encoders.Hex.toHexString(this.getHash());
    }

    public String toString() {
        return this.toJsonObject().toString();
    }

    public String toStringPretty() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this.toJsonObject());
    }

    public ECKey getEcKeyPub() {
        return this.signature.getEcKeyPub();
    }

    public byte[] getPubKey() {
        return this.getEcKeyPub().getPubKey();
    }

    public String getPubKeyHexString() {
        return Hex.toHexString(this.getEcKeyPub().getPubKey());
    }

    public byte[] getAddress() {
        return this.getEcKeyPub().getAddress();
    }

    public String getAddressToString() {
        return Hex.toHexString(getAddress());
    }

    public long length() throws IOException {
        return this.header.length() + this.signature.length() + this.body.length();
    }

    @Override
    public Block clone() throws CloneNotSupportedException {
        Block block = (Block) super.clone();
        block.header = this.header.clone();
        block.signature = this.signature.clone();
        block.body = this.body.clone();

        return block;
    }

}
