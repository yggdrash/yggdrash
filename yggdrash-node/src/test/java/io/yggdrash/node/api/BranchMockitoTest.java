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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.gateway.dto.BranchDto;
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

            String consensusString = new StringBuilder()
                    .append("{\"consensus\": {")
                    .append("    \"algorithm\": \"pbft\",")
                    .append("    \"period\": \"2\",")
                    .append("    \"validator\": {")
                    .append("      \"527e5997e79cc0935d9d86a444380a11cdc296b6bcce2c6df5e5439a3cd7bffb945e77aacf881f36a668284984b628063f5d18a214002ac7ad308e04b67bcad8\": {")
                    .append("        \"host\": \"127.0.0.1\",")
                    .append("        \"port\": \"32911\"")
                    .append("      },")
                    .append("      \"e12133df65a2e7dec4310f3511b1fa6b35599770e900ffb50f795f2a49d0a22b63e013a393affe971ea4db08cc491118a8a93719c3c1f55f2a12af21886d294d\": {")
                    .append("        \"host\": \"127.0.0.1\",")
                    .append("        \"port\": \"32912\"")
                    .append("      },")
                    .append("      \"8d69860332aa6202df489581fd618fc085a6a5af89964d9e556a398d232816c9618fe15e90015d0a2d15037c91587b79465106f145c0f4db6d18b105659d2bc8\": {")
                    .append("        \"host\": \"127.0.0.1\",")
                    .append("        \"port\": \"32913\"")
                    .append("      },")
                    .append("      \"b49fbee055a4b3bd2123a60b24f29d69bc0947e45a75eb4880fe9c5b07904c650729e5edcdaff2523c8839889925079963186bd38c22c96433bdbf4465960527\": {")
                    .append("        \"host\": \"127.0.0.1\",")
                    .append("        \"port\": \"32914\"")
                    .append("      }")
                    .append("    }")
                    .append("  }}").toString();
            JsonObject consensus = new Gson().fromJson(consensusString, JsonObject.class);
            json.add("consensus", consensus.get("consensus").getAsJsonObject());

            Branch branch = Branch.of(json);
            assertThat(entry.getKey()).isEqualTo(branch.getBranchId().toString());
        }
    }
}
