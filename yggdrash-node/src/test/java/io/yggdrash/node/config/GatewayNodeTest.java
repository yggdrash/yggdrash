/*
 * Copyright 2019 Akashic Foundation
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

package io.yggdrash.node.config;

import io.yggdrash.TestConstants;
import io.yggdrash.common.config.Constants;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.gateway.controller.BranchController;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({Constants.ActiveProfiles.GATEWAY, Constants.ActiveProfiles.NODE})
public class GatewayNodeTest extends TestConstants.CiTest {

    @Autowired
    private BranchGroup branchGroup;

    @Autowired(required = false)
    private BranchController branchController;

    @Ignore
    @Test
    public void shouldBeNotEmptyBranch() {
        assertThat(branchGroup.getAllBranch()).isNotEmpty();
    }

    @Test
    public void shouldBeActivateGatewayProfile() {
        assertThat(branchController).isNotNull();
    }

}