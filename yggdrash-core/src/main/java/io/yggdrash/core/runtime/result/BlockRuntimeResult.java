/*
 * Copyright 2019 Akashic Foundation
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

package io.yggdrash.core.runtime.result;

import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.contract.TransactionReceipt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockRuntimeResult {
    Map<String, JsonObject> blockResult = new HashMap<>();
    List<TransactionReceipt> txReceipts = new ArrayList<>();
    BlockHusk block;


    public BlockRuntimeResult(BlockHusk block) {
        this.block = block;
    }

    public void setBlockResult(Set<Map.Entry<String, JsonObject>> values) {
        values.stream().forEach(entry -> blockResult.put(entry.getKey(), entry.getValue()));
    }

    public void addTxReceipt(TransactionReceipt txReceipt) {
        txReceipts.add(txReceipt);
    }

    public List<TransactionReceipt> getTxReceipts() {
        return this.txReceipts;
    }

    public Map<String, JsonObject> getBlockResult() {
        return this.blockResult;
    }

    public BlockHusk getOriginBlock() {
        return this.block;
    }



}
