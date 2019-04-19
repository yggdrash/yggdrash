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

import com.google.gson.JsonArray;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.common.utils.SerializationUtil;

import java.util.Arrays;

public class TransactionBody {

    private final JsonArray body = new JsonArray();

    private byte[] binary;

    public TransactionBody(JsonArray body) {
        this.body.addAll(body);
    }

    public TransactionBody(byte[] bodyBytes) {
        this(SerializationUtil.deserializeString(bodyBytes));
    }

    public TransactionBody(String body) {
        this.body.addAll(JsonUtil.parseJsonArray(body));
    }

    public JsonArray getBody() {
        return this.body;
    }

    long getBodyCount() {
        return this.body.size();
    }

    public long length() {
        return toBinary().length;
    }

    public byte[] getBodyHash() {
        return HashUtil.sha3(toBinary());
    }

    public byte[] toBinary() {
        if (binary != null) {
            return binary;
        }
        binary = SerializationUtil.serializeString(body.toString());
        return binary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransactionBody that = (TransactionBody) o;
        return Arrays.equals(toBinary(), that.toBinary());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(toBinary());
    }
}
