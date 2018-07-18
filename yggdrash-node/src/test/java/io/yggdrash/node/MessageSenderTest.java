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

import io.yggdrash.core.net.Peer;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class MessageSenderTest {

    MessageSender messageSender;

    @Before
    public void setUp() {
        this.messageSender = new MessageSender();
    }

    @Test
    public void syncBlock() throws IOException {
        assert messageSender.syncBlock(0).isEmpty();
    }

    @Test
    public void addActivePeerTest() {
        messageSender.newPeer(Peer.valueOf("ynode://75bff16c@localhost:9999"));
        assertEquals(0, messageSender.getActivePeerList().size());
    }
}
