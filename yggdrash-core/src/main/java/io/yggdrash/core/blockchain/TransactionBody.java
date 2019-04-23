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
import io.yggdrash.common.utils.JsonUtil;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;

public class TransactionBody {

    private JsonObject body; //TODO Change modifier to final

    public TransactionBody(JsonObject body) {
        this.body = body;
    }

    public TransactionBody(String body) {
        this.body = JsonUtil.parseJsonObject(body);
    }

    public TransactionBody(byte[] bodyBytes) {
        this(new String(bodyBytes, StandardCharsets.UTF_8));
    }

    public JsonObject getBody() {
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
}
