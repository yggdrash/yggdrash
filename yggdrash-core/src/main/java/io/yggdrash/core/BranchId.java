/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.core.exception.NotValidateException;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BranchId {

    public static final String STEM = "9435b0e642e99606cd9cdf362e63fb2d46fca12b";
    public static final String YEED = "fc35bcd26643c34fbb3a03a122d0b586d3487944";
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
        return new BranchId(Hex.decode(hash));
    }

    public static BranchId of(byte[] hash) {
        return new BranchId(hash);
    }

    public static BranchId of(JsonObject branch) {
        return new BranchId(branch);
    }

    public static BranchId stem() {
        return BranchId.of(STEM);
    }

    public static BranchId yeed() {
        return BranchId.of(YEED);
    }

    private static byte[] getRawBranch(JsonObject branch) {
        ByteArrayOutputStream branchStream = new ByteArrayOutputStream();
        try {
            branchStream.write(branch.get("name").getAsString().getBytes());
            branchStream.write(branch.get("property").getAsString().getBytes());
            branchStream.write(branch.get("type").getAsString().getBytes());
            branchStream.write(branch.get("timestamp").getAsString().getBytes());
            branchStream.write(branch.get("version").getAsString().getBytes());
            branchStream.write(branch.get("reference_address").getAsString().getBytes());
            branchStream.write(branch.get("reserve_address").getAsString().getBytes());
        } catch (IOException e) {
            throw new NotValidateException(e);
        }
        return branchStream.toByteArray();
    }
}
