package io.yggdrash.core;

import com.google.gson.JsonArray;
import org.spongycastle.util.encoders.Hex;

public class TransactionBody {

    private JsonArray body;
    private long length;

    public TransactionBody(JsonArray body) {
        this.body = body;
        this.length = body.toString().length();
    }


    public long length() {
        return this.length;
    }

    public String getHexString() {
        return Hex.toHexString(body.toString().getBytes());
    }

    public byte[] getBinary() {
        return body.toString().getBytes();
    }

    public JsonArray getBody() {
        return body;
    }

}
