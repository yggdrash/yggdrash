/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node;

import com.google.gson.JsonObject;
import io.yggdrash.core.Account;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.net.Peer;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class MessageSenderTest {

    private MessageSender messageSender;
    private Transaction tx;
    private Block block;

    @Before
    public void setUp() throws IOException {
        Account author = new Account();
        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");
        this.tx = new Transaction(author, json);
        BlockBody sampleBody = new BlockBody(Collections.singletonList(tx));

        BlockHeader genesisBlockHeader = new BlockHeader.Builder()
                .blockBody(sampleBody)
                .prevBlock(null)
                .build(author);
        this.block = new Block(genesisBlockHeader, sampleBody);
        this.messageSender = new MessageSender();

    }

    @Test
    public void syncBlock() throws IOException {
        messageSender.newBlock(block);
        assert messageSender.syncBlock(0).isEmpty();
    }

    @Test
    public void syncTransaction() throws IOException {
        messageSender.newTransaction(tx);
        assert messageSender.syncTransaction().isEmpty();
    }

    @Test
    public void addActivePeerTest() {
        messageSender.newPeer(Peer.valueOf("ynode://75bff16c@localhost:9999"));
        assert messageSender.getActivePeerList().isEmpty();
    }

    @Test
    public void broadcastPeerTest() {
        assert messageSender.broadcastPeer("ynode://75bff16c@localhost:9999").isEmpty();
    }
}
