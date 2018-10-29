package io.yggdrash.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.yggdrash.common.crypto.HashUtil;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;

public class TransactionBody implements Cloneable {

    private final JsonArray body;

    public TransactionBody(JsonArray body) {

        this.body = body;
    }

    public TransactionBody(String body) {
        this.body = new Gson().fromJson(body, JsonArray.class);
    }

    public TransactionBody(byte[] bodyBytes) {
        this(new String(bodyBytes, StandardCharsets.UTF_8));
    }

    public JsonArray getBody() {
        return this.body;
    }

    long getBodyCount() {
        return this.body.size();
    }

    public long length() {
        return this.body.toString().length();
    }

    public byte[] getBodyHash() {
        return HashUtil.sha3(this.toBinary());
    }

    public String toString() {
        return this.body.toString();
    }

    public String toHexString() {
        return Hex.toHexString(this.body.toString().getBytes());
    }

    public byte[] toBinary() {
        return this.body.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public TransactionBody clone() throws CloneNotSupportedException {
        //todo: check body data whether clone or not
        return (TransactionBody) super.clone();
    }
}
