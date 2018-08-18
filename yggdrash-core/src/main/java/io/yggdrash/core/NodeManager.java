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

package io.yggdrash.core;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.contract.StateStore;
import io.yggdrash.core.event.PeerEventListener;

import java.util.List;
import java.util.Set;

public interface NodeManager extends PeerEventListener {

    void init();

    TransactionHusk addTransaction(TransactionHusk tx);

    List<TransactionHusk> getTransactionList();

    TransactionHusk getTxByHash(Sha3Hash hash);

    TransactionHusk getTxByHash(String id);

    BlockHusk generateBlock();

    BlockHusk addBlock(BlockHusk block);

    Set<BlockHusk> getBlocks();

    BlockHusk getBlockByIndexOrHash(String indexOrHash);

    String getNodeUri();

    void addPeer(String peer);

    void removePeer(String peer);

    List<String> getPeerUriList();

    Wallet getWallet();

    StateStore getStateStore();

    Integer getBalanceOf(String address);
}
