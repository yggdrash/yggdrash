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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"yggdrash.node.seed=true", "yggdrash.node.chain.enabled=false" })
@IfProfileValue(name = "spring.profiles.active", value = TestConstants.CI_TEST)
public class BootstrapNodeTest {
    private static final Logger log = LoggerFactory.getLogger(BootstrapNodeTest.class);

    @Autowired
    private BranchGroup branchGroup;

//    @Test
//    public void shouldBeEmptyBranch() {
//        assertThat(branchGroup.getAllBranch()).isEmpty();
//    }
}