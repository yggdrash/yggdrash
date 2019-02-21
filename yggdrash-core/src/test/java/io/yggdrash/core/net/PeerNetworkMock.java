/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.p2p.PeerHandler;
import io.yggdrash.core.p2p.PeerHandlerMock;

import java.util.Collections;
import java.util.List;

public class PeerNetworkMock implements PeerNetwork {
    static final PeerNetwork mock = new PeerNetworkMock();

    @Override
    public void init() {
    }

    @Override
    public void addNetwork(BranchId branchId) {
    }

    @Override
    public List<PeerHandler> getHandlerList(BranchId branchId) {
        return Collections.singletonList(PeerHandlerMock.dummy());
    }

    @Override
    public void receivedTransaction(TransactionHusk tx) {
    }

    @Override
    public void chainedBlock(BlockHusk block) {
    }
}
