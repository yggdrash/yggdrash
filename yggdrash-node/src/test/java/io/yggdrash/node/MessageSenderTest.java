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
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.node.config.NodeProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MessageSenderTest {

    MessageSender messageSender;

    PeerGroup peerGroup;

    @Mock
    NodeProperties nodeProperties;

    @Before
    public void setUp() {
        when(nodeProperties.getSeedPeerList())
                .thenReturn(Arrays.asList("ynode://0462b608@localhost:9090"));
        this.peerGroup = new PeerGroup();
        this.messageSender = new MessageSender(peerGroup, nodeProperties);
        messageSender.init();
    }

    @Test
    public void getPeerIdList() {
        peerGroup.addPeer(Peer.valueOf("ynode://0462b608@localhost:9090"));
        messageSender.getPeerIdList();
        assertThat(messageSender.getPeerIdList()).contains("0462b608");
    }

}