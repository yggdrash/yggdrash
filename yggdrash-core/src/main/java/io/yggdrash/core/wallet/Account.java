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

import io.yggdrash.common.crypto.ECKey;
import org.spongycastle.util.encoders.Hex;

/**
 * Account Class.
 */
public class Account {

    // <Variable>
    private final ECKey key;
    private final byte[] address;

    /**
     * Account Constructor.
     * - generate wallet with new key
     */
    public Account() {
        this.key = new ECKey();
        this.address = this.key.getAddress();
    }

    /**
     * get Account Key.
     *
     * @return key
     */
    public ECKey getKey() {
        return key;
    }

    /**
     * get Account Address.
     *
     * @return address
     */
    public byte[] getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "Account{"
                + "publicKey=" + Hex.toHexString(key.getPubKey())
                + ",address=" + Hex.toHexString(address)
                + '}';
    }

}
