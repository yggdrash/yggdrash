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

package io.yggdrash.core.wallet;

import io.yggdrash.common.crypto.HashUtil;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;

public class Address {
    private final byte[] data;

    public static final Address NULL_ADDRESS = new Address(new byte[20], true);

    public Address(byte[] data) {
        this(data, false);
    }

    private Address(byte[] data, boolean hashed) {
        if (hashed) {
            this.data = data;
        } else {
            this.data = HashUtil.sha3omit12(Arrays.copyOfRange(data, 1, data.length));
        }
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
        Address address = (Address) o;
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

    public static Address of(String addr) {
        return new Address(Hex.decode(addr), true);
    }
}
