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
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.utils.SerializationUtil;
import io.yggdrash.core.exception.DecodeException;
import org.spongycastle.util.encoders.DecoderException;
import org.spongycastle.util.encoders.Hex;

public class BranchId {

    public static final BranchId NULL = new BranchId(Constants.EMPTY_BRANCH);

    private final Sha3Hash id;

    public BranchId(Sha3Hash hash) {
        this.id = hash;
    }

    private BranchId(byte[] bytes) {
        this(Sha3Hash.createByHashed(bytes));
    }

    private BranchId(JsonObject branch) {
        this(HashUtil.sha3omit12(getRawBranch(branch)));
    }

    public byte[] getBytes() {
        return this.id.getBytes();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BranchId branchId = (BranchId) o;
        return id.equals(branchId.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }

    public static BranchId of(String hash) {
        try {
            return new BranchId(Hex.decode(hash));
        } catch (DecoderException e) {
            throw new DecodeException.BranchIdNotHexString();
        }
    }

    public static BranchId of(byte[] hash) {
        return new BranchId(hash);
    }

    public static BranchId of(JsonObject branch) {
        return new BranchId(branch);
    }

    private static byte[] getRawBranch(JsonObject branch) {
        return SerializationUtil.serializeString(branch.toString());
    }
}
