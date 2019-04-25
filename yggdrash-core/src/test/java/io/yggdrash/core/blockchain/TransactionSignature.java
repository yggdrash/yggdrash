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
import io.yggdrash.core.wallet.Wallet;
import org.spongycastle.util.encoders.Hex;

public class TransactionSignature {

    private final byte[] signature;

    TransactionSignature(Wallet wallet, byte[] headerHash) {
        this(wallet.sign(headerHash, true));
    }

    private TransactionSignature(byte[] signature) {
        this.signature = signature;
    }

    public byte[] getSignature() {
        return this.signature;
    }

    private JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("signature", Hex.toHexString(this.signature));

        return jsonObject;
    }

    @Override
    public String toString() {
        return this.toJsonObject().toString();
    }
}
