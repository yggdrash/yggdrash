/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.node.api;

import io.yggdrash.TestConstants;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.yggdrash.node.api.JsonRpcConfig.BRANCH_API;
import static org.assertj.core.api.Assertions.assertThat;

public class BranchApiImplTest {

    private static final Logger log = LoggerFactory.getLogger(BranchApiImplTest.class);
    private final String branchId = TestConstants.yggdrash().toString();

    @Test
    public void branchApiIsNotNull() {
        assertThat(BRANCH_API).isNotNull();
    }

    @Test
    public void getBranchesTest() {
        try {
            assertThat(BRANCH_API.getBranches()).isNotNull();
        } catch (Exception exception) {
            log.debug("getBranchesTest :: exception : " + exception);
        }
    }

    @Test
    public void getValidatorsTest() {
        try {
            assertThat(BRANCH_API.getValidators(branchId)).isNotNull();
        } catch (Exception exception) {
            log.debug("getValidatorsTest :: exception : " + exception);
        }
    }
}