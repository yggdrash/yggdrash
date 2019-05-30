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

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchContract;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.wallet.Wallet;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TestConstants {

    private static BranchId YGGDRASH_BRANCH_ID;

    static ContractVersion STEM_CONTRACT;
    public static ContractVersion YEED_CONTRACT;
    public static Branch TEST_BRANCH;
    public static File branchFile;

    public static final String TRANSFER_TO = "e1980adeafbb9ac6c9be60955484ab1547ab0b76";

    private static final String CI_TEST = "ci";
    private static final String SLOW_TEST = "slow";
    private static final String PERFORMANCE_TEST = "performance";
    private static final String CONSOLE_TEST = "console";
    private static final String PROFILE = System.getProperty("spring.profiles.active");

    private static Wallet wallet;

    private TestConstants() {
    }

    static {
        try {
            wallet = new Wallet(new DefaultConfig(), "Aa1234567890!");
            branchFile = new File("../yggdrash-core/src/main/resources", "branch-yggdrash.json");
            if (!branchFile.exists()) {
                branchFile = new File("yggdrash-core/src/main/resources", "branch-yggdrash.json");
            }
        } catch (Exception e) {
            throw new InvalidSignatureException(e);
        }
    }

    public static BranchId yggdrash() {
        if (YGGDRASH_BRANCH_ID != null) {
            return YGGDRASH_BRANCH_ID;
        }

        try (InputStream is = new FileInputStream(branchFile)) {
            Branch yggdrashBranch = Branch.of(is);
            TEST_BRANCH = yggdrashBranch;
            YGGDRASH_BRANCH_ID = yggdrashBranch.getBranchId();
            for (BranchContract bc : yggdrashBranch.getBranchContracts()) {
                if ("STEM".equals(bc.getName())) {
                    STEM_CONTRACT = bc.getContractVersion();
                }
                if ("YEED".equals(bc.getName())) {
                    YEED_CONTRACT = bc.getContractVersion();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            YGGDRASH_BRANCH_ID = null;
        }
        return YGGDRASH_BRANCH_ID;
    }

    public static Wallet wallet() {
        return wallet;
    }

    public static Wallet wallet(String path) throws IOException, InvalidCipherTextException {
        wallet = new Wallet(path, "As1234567890!");
        return wallet;
    }

    public static class SlowTest {
        @BeforeClass
        public static void apply() {
            Assume.assumeTrue(contains(SLOW_TEST));
        }
    }

    public static class PerformanceTest {
        @BeforeClass
        public static void apply() {
            Assume.assumeTrue(contains(PERFORMANCE_TEST));
        }
    }

    // for getting password with console
    public static class ConsoleTest {
        @BeforeClass
        public static void apply() {
            Assume.assumeTrue(contains(CONSOLE_TEST));
        }
    }

    public static class CiTest {
        @BeforeClass
        public static void apply() {
            Assume.assumeTrue(contains(CI_TEST));
        }
    }

    private static boolean contains(String profile) {
        if (PROFILE == null) {
            return false;
        }
        return PROFILE.contains(profile);
    }
}
