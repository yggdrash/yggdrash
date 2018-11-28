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

package io.yggdrash.core.contract;

import io.yggdrash.common.crypto.HashUtil;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;

public class ContractId {

    private final byte[] data;

    private ContractId(byte[] data) {
        this.data = data;
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
        ContractId address = (ContractId) o;
        return Arrays.equals(data, address.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        return Hex.toHexString(data);
    }

    static ContractId of(byte[] contractBytes) {
        return new ContractId(HashUtil.sha1(contractBytes));
    }

    public static ContractId of(String hexString) {
        return new ContractId(Hex.decode(hexString));
    }
}
