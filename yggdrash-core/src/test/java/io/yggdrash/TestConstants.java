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
import org.junit.Assume;
import org.junit.BeforeClass;

public class TestConstants {

    public static final BranchId STEM = BranchId.of("68932de2b04e1aee47b78e8acd2de1f9036ecd88");
    public static final BranchId YEED = BranchId.of("61dcf9cf6ed382f39f56a1094e2de4d9aa54bf94");

    public static final String TRANSFER_TO = "e1980adeafbb9ac6c9be60955484ab1547ab0b76";

    public static final String CI_TEST = "ci";
    private static final String SLOW_TEST = "slow";
    private static final String PERFORMANCE_TEST = "performance";
    private static final String PROFILE = System.getProperty("spring.profiles.active");

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

    public static class SlowTest {
        @BeforeClass
        public static void apply() {
            Assume.assumeTrue(SLOW_TEST.equals(PROFILE));
        }
    }

    public static class PerformanceTest {
        @BeforeClass
        public static void apply() {
            Assume.assumeTrue(PERFORMANCE_TEST.equals(PROFILE));
        }
    }

    public static class CiTest {
        @BeforeClass
        public static void apply() {
            Assume.assumeTrue(CI_TEST.equals(PROFILE));
        }
    }
}
