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

package io.yggdrash.node.api;

import com.google.gson.JsonObject;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.node.api.dto.BranchDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BranchMockitoTest {

    @Mock
    private BranchGroup branchGroupMock;

    @Test
    public void getBranchesTest() {
        BranchApiImpl branchApiImpl = new BranchApiImpl(branchGroupMock);
        BlockChain blockChain = BlockChainTestUtils.createBlockChain(false);
        when(branchGroupMock.getAllBranch()).thenReturn(Collections.singleton(blockChain));
        for (Map.Entry<String, BranchDto> entry : branchApiImpl.getBranches().entrySet()) {
            String jsonString = JsonUtil.convertObjToString(entry.getValue());
            JsonObject json = JsonUtil.parseJsonObject(jsonString);
            Branch branch = Branch.of(json);
            assertThat(entry.getKey()).isEqualTo(branch.getBranchId().toString());
        }
    }
}
