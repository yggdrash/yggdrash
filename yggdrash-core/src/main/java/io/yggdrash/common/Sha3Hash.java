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

package io.yggdrash.common;

import io.yggdrash.crypto.HashUtil;
import org.spongycastle.util.encoders.Hex;

import java.io.Serializable;
import java.util.Arrays;

public class Sha3Hash implements Serializable {
    private final byte[] data;

    private Sha3Hash(byte[] data, boolean hashed) {
        if (hashed) {
            this.data = data;
        } else {
            this.data = HashUtil.sha3(data);
        }
    }

    public Sha3Hash(byte[] data) {
        this.data = HashUtil.sha3(data);
    }

    public Sha3Hash(String hash) {
        this.data = Hex.decode(hash);
    }

    public byte[] getBytes() {
        return this.data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Sha3Hash sha3Hash = (Sha3Hash) o;
        return Arrays.equals(data, sha3Hash.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        return Hex.toHexString(data);
    }

    public static Sha3Hash createByHashed(byte[] hashedData) {
        return new Sha3Hash(hashedData, true);
    }
}
