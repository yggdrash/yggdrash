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

package io.yggdrash;

import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.wallet.Wallet;

public class TestConstants {
    public static final BranchId STEM = BranchId.of("91b29a1453258d72ca6fbbcabb8dca10cca944fb");
    public static final BranchId YEED = BranchId.of("d872b5a338b824dc56abc6015543496670d81c1b");

    public static final String TRANSFER_TO = "e1980adeafbb9ac6c9be60955484ab1547ab0b76";
    private static final Wallet wallet;

    private TestConstants() {}

    static {
        try {
            wallet = new Wallet();
        } catch (Exception e) {
            throw new InvalidSignatureException(e);
        }
    }

    public static Wallet wallet() {
        return wallet;
    }
}
