/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.core.blockchain;

import java.util.Set;

public interface TransactionIndexer {

    // Analyzes, indexes and stores a batch of transactions
    // Batch groups together multiple index operations to be performed at the same time
    void addBatch(Set<TransactionHusk> txs);

    // Analyzes, indexes and stores a single transaction
    void index(TransactionHusk tx);

    // Returns the transaction specified by hash or null if the transaction is not indexed or stored
    TransactionHusk getTxByHash(BranchId branchId, byte[] hash);

    // Allows you to query for transactions
    Set<TransactionHusk> search(TransactionQuery txQuery);
}
