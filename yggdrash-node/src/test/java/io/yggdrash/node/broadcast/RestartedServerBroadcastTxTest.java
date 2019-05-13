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

package io.yggdrash.node.broadcast;

import io.yggdrash.TestConstants;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.blockchain.BlockChainManager;
import io.yggdrash.node.TcpNodeTesting;
import io.yggdrash.node.TestNode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RestartedServerBroadcastTxTest extends TcpNodeTesting {

    @Test
    public void test() {
        TestConstants.SlowTest.apply();
        // arrange
        bootstrapNodes(3, true);

        TestNode server = nodeList.get(1);
        TestNode client = nodeList.get(2);
        // add routing to server
        client.peerTableGroup.addPeer(TestConstants.yggdrash(), server.peerTableGroup.getOwner());

        // act & assert
        // broadcast success
        client.generateBlock();
        Utils.sleep(100);

        BlockChainManager serverBlockChainManager = server.getDefaultBranch().getBlockChainManager();
        assertThat(serverBlockChainManager.getLastIndex()).isEqualTo(1);

        // broadcast fail - server shutdown
        server.shutdown();
        client.generateBlock();
        Utils.sleep(100);
        assertThat(serverBlockChainManager.getLastIndex()).isEqualTo(1);
        // broadcast fail - server restart
        createAndStartServer(server);
        client.generateBlock();
        Utils.sleep(100);
        server.shutdown();
        // assert
        assertThat(serverBlockChainManager.getLastIndex())
                .isEqualTo(client.getDefaultBranch().getBlockChainManager().getLastIndex());
    }
}
