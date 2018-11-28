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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.contract.ContractId;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.wallet.Address;
import io.yggdrash.core.wallet.Wallet;
import org.apache.commons.io.IOUtils;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Arrays;

public class Branch {

    private final BranchId branchId;
    private final String name;
    private final String symbol;
    private final String property;
    private final BranchType type;
    private final String description;
    private final ContractId contractId;
    private final long timestamp;
    private final Address owner;
    private final Sha3Hash rawForSign;
    private final Sha3Hash signature;
    private final JsonObject json;

    private Branch(JsonObject json) {
        this.json = json;
        this.branchId = BranchId.of(json);
        this.name = json.get("name").getAsString();
        this.symbol = json.get("symbol").getAsString();
        this.property = json.get("property").getAsString();
        this.type = branchType();
        this.description = json.get("description").getAsString();
        this.contractId = ContractId.of(json.get("contractId").getAsString());
        String timestampHex = json.get("timestamp").getAsString();
        this.timestamp = ByteUtil.byteArrayToLong(Hex.decode(timestampHex));
        this.rawForSign = rawHashForSign();
        this.signature = Sha3Hash.createByHashed(Hex.decode(json.get("signature").getAsString()));
        this.owner = Address.of(json.get("owner").getAsString());
    }

    public BranchId getBranchId() {
        return branchId;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getProperty() {
        return property;
    }

    public BranchType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public ContractId getContractId() {
        return contractId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Address getOwner() {
        return owner;
    }

    public Sha3Hash getSignature() {
        return signature;
    }

    public JsonObject getJson() {
        return json;
    }

    public boolean isStem() {
        return symbol != null && Constants.STEM.equals(symbol);
    }

    public boolean verify() {
        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(signature.getBytes());
        try {
            ECKey ecKeyPub = ECKey.signatureToKey(rawForSign.getBytes(), ecdsaSignature);
            if (ecKeyPub.verify(rawForSign.getBytes(), ecdsaSignature)) {
                return new Address(ecKeyPub.getAddress()).equals(owner);
            } else {
                return false;
            }
        } catch (SignatureException e) {
            throw new InvalidSignatureException(e);
        }
    }

    private BranchType branchType() {
        JsonElement type = json.get("type");
        if (type == null || "".equals(type.getAsString())) {
            return BranchType.TEST;
        } else {
            return BranchType.of(type.getAsString());
        }
    }

    private Sha3Hash rawHashForSign() {
        String signatureHex = json.remove("signature").getAsString();
        byte[] raw = json.toString().getBytes(StandardCharsets.UTF_8);
        json.addProperty("signature", signatureHex);
        return new Sha3Hash(raw);
    }

    public static JsonObject signBranch(Wallet wallet, JsonObject raw) {
        if (!raw.has("signature")) {
            raw.addProperty("owner", wallet.getHexAddress());
            Sha3Hash hashForSign = new Sha3Hash(raw.toString().getBytes(StandardCharsets.UTF_8));
            byte[] signature = wallet.signHashedData(hashForSign.getBytes());
            raw.addProperty("signature", Hex.toHexString(signature));
        }
        return raw;
    }

    public static Branch of(InputStream is) throws IOException {
        String branchString = IOUtils.toString(is, StandardCharsets.UTF_8);
        JsonObject json = Utils.parseJsonObject(branchString);
        return of(json);
    }

    public static Branch of(JsonObject json) {
        return new Branch(json);
    }

    public enum BranchType {
        IMMUNITY, MUTABLE, INSTANT, PRIVATE, TEST;

        static BranchType of(String val) {
            return Arrays.stream(BranchType.values())
                    .filter(e -> e.toString().toLowerCase().equals(val))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            String.format("Unsupported type %s.", val)));
        }
    }
}
