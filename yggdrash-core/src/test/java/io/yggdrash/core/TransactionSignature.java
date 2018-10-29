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

package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.core.account.Wallet;
import org.spongycastle.util.encoders.Hex;

public class TransactionSignature implements Cloneable {

    private final byte[] signature;

    TransactionSignature(byte[] signature) {
        this.signature = signature;
    }

    TransactionSignature(JsonObject jsonObject) {
        this.signature = Hex.decode(jsonObject.get("signature").getAsString());
    }

    public TransactionSignature(Wallet wallet, byte[] headerHash) {
        this(wallet.signHashedData(headerHash));
    }

    public byte[] getSignature() {
        return this.signature;
    }

    String getSignatureHexString() {
        return Hex.toHexString(this.signature);
    }

    JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("signature", Hex.toHexString(this.signature));

        return jsonObject;
    }

    public String toString() {
        return this.toJsonObject().toString();
    }

    @Override
    public TransactionSignature clone() throws CloneNotSupportedException {
        return (TransactionSignature) super.clone();
    }

}
