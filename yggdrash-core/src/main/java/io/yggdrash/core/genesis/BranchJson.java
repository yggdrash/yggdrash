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

package io.yggdrash.core.genesis;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.account.Wallet;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.exception.NotValidateException;
import org.apache.commons.io.IOUtils;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Map;

public class BranchJson {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public String name;
    public String symbol;
    public String property;
    public String description;
    public String contractId;
    public Map<String, Object> genesis;
    public String timestamp;
    public String owner;
    public String signature;
    public String branchId;

    long longTimestamp() {
        return ByteUtil.byteArrayToLong(Hex.decode(timestamp));
    }

    BranchId branchId() {
        return BranchId.of(branchId);
    }

    String getContractId() {
        return contractId;
    }

    boolean isStem() {
        return symbol != null && "STEM".equals(symbol);
    }

    JsonObject toJsonObject() {
        try {
            String jsonString = MAPPER.writeValueAsString(this);
            return Utils.parseJsonObject(jsonString);
        } catch (JsonProcessingException e) {
            throw new NotValidateException(e);
        }
    }

    public static BranchJson toBranchJson(JsonObject jsonObjectBranch) throws IOException {
        return MAPPER.readValue(jsonObjectBranch.toString(), BranchJson.class);
    }

    public static BranchJson toBranchJson(InputStream branch) throws IOException {
        String branchString = IOUtils.toString(branch, StandardCharsets.UTF_8);
        JsonObject jsonObjectBranch = Utils.parseJsonObject(branchString);
        if (!verify(jsonObjectBranch)) {
            throw new InvalidSignatureException();
        }
        BranchJson branchJson = MAPPER.readValue(branchString, BranchJson.class);
        branchJson.branchId = BranchId.of(jsonObjectBranch).toString();
        return branchJson;
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

    private static boolean verify(JsonObject jsonObjectBranch) {
        if (!jsonObjectBranch.has("signature") || !jsonObjectBranch.has("owner")) {
            return false;
        }

        String signature = jsonObjectBranch.remove("signature").getAsString();
        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(Hex.decode(signature));
        byte[] rawForsing = jsonObjectBranch.toString().getBytes(StandardCharsets.UTF_8);
        byte[] hashedHeader = new Sha3Hash(rawForsing).getBytes();
        jsonObjectBranch.addProperty("signature", signature);

        try {
            ECKey ecKeyPub = ECKey.signatureToKey(hashedHeader, ecdsaSignature);
            if (ecKeyPub.verify(hashedHeader, ecdsaSignature)) {
                String owner = jsonObjectBranch.get("owner").getAsString();
                return Hex.toHexString(ecKeyPub.getAddress()).equals(owner);
            } else {
                return false;
            }
        } catch (SignatureException e) {
            throw new InvalidSignatureException(e);
        }
    }
}
