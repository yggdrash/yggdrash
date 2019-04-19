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

package io.yggdrash.core.p2p;

import org.junit.Assert;
import org.junit.Test;

public class PeerIdTest {

    private static final String ynodeUri1
            = "ynode://9ea9225f0b7db3c697c0a2e09cdd65046899058d16f73378c1559d61aa3e10cd5dc933714"
            + "2728f5a02faadafab2b926e2998d5bc2b62c2183fab75ca996de2ce@localhost:32918";
    private static final String ynodeUri2
            = "ynode://9ea9225f0b7db3c697c0a2e09cdd65046899058d16f73378c1559d61aa3e10cd5dc933714"
            + "2728f5a02faadafab2b926e2998d5bc2b62c2183fab75ca996de2ce@localhost:32919";

    @Test
    public void of() {
        PeerId peerId1 = PeerId.of(ynodeUri1);
        byte[] b = peerId1.getBytes();
        PeerId peerId2 = PeerId.of(b);
        Assert.assertEquals(peerId1, peerId2);
    }

    @Test
    public void shouldBeSameDistance() {
        PeerId peerId1 = PeerId.of(ynodeUri1);
        PeerId peerId2 = PeerId.of(ynodeUri2);
        Assert.assertEquals(peerId1.distanceTo(peerId2.getBytes()), peerId2.distanceTo(peerId1.getBytes()));
    }
}