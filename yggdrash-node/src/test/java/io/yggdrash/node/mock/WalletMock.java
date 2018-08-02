/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.mock;

import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.Wallet;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;

public class WalletMock {

    private static final Wallet wallet = readWallet();

    public static Wallet get() {
        return wallet;
    }

    public static Transaction sign(Transaction tx) {
        byte[] data = tx.getHeader().getDataHashForSigning();
        byte[] signed = wallet.signHashedData(data);
        tx.getHeader().setSignature(signed);
        return tx;
    }

    private static Wallet readWallet() {
        try {
            return new Wallet(new DefaultConfig());
        } catch (IOException | InvalidCipherTextException e) {
            throw new RuntimeException(e);
        }
    }
}
