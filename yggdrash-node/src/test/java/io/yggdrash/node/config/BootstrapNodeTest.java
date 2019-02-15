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
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.gateway.controller.BranchController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"yggdrash.node.seed=true", "yggdrash.node.chain.enabled=false"})
@DirtiesContext
public class BootstrapNodeTest extends TestConstants.CiTest {

    @Autowired
    private BranchGroup branchGroup;

    @Autowired(required = false)
    private BranchController branchController;

    @Test
    public void shouldBeEmptyBranch() {
        assertThat(branchGroup.getAllBranch()).isEmpty();
    }

    @Test
    public void shouldBeNotActivateGatewayProfile() {
        assertThat(branchController).isNull();
    }
}