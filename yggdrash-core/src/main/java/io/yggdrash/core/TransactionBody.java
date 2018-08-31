package io.yggdrash.core;

import com.google.gson.JsonArray;
import io.yggdrash.crypto.HashUtil;
import org.spongycastle.util.encoders.Hex;

public class TransactionBody implements Cloneable {

    private JsonArray body;

    public TransactionBody(JsonArray body) {

        this.body = body;
    }

    public JsonArray getBody() {
        return this.body;
    }

    public byte[] getBinary() {
        return this.body.toString().getBytes();
    }

    public String getHexString() {
        return Hex.toHexString(this.body.toString().getBytes());
    }

    public long getBodyCount() {
        return this.body.size();
    }

    public long length() {
        return this.body.toString().length();
    }

    public byte[] getBodyHash() {
        return HashUtil.sha3(this.getBinary());
    }

    public String toString() {
        return this.body.toString();
    }

    @Override
    public TransactionBody clone() throws CloneNotSupportedException {
        //todo: check body data whether clone or not
        return (TransactionBody) super.clone();
    }
}
