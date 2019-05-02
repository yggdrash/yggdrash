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

package io.yggdrash.common.contract;

import io.yggdrash.common.crypto.HashUtil;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;

public class ContractVersion {
    private final byte[] data;
    private boolean isSystem;

    private ContractVersion(byte[] data) {
        this.data = data;
    }

    private ContractVersion(byte[] data, boolean isSystem) {
        this.data = data;
        this.isSystem = isSystem;
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
        ContractVersion address = (ContractVersion) o;
        return Arrays.equals(data, address.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        if (isSystem) {
            return new String(data);
        }
        return Hex.toHexString(data);
    }

    public static ContractVersion of(byte[] contractBytes) {
        //TODO SHA1 hash function is now completely unsafe.
        return new ContractVersion(HashUtil.sha1(contractBytes));
    }

    public static ContractVersion of(String hexString) {
        return new ContractVersion(Hex.decode(hexString));
    }

    public static ContractVersion ofNonHex(String nonHexString) {
        return new ContractVersion(nonHexString.getBytes(), true);
    }
}
