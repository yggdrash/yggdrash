/*
 * Copyright 2019 Akashic Foundation
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

package io.yggdrash.gateway;

import com.google.gson.JsonObject;
import com.google.protobuf.util.Timestamps;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.store.OutputStore;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchEventListener;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBody;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.gateway.dto.BlockDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 블록체인에서 발생되는 정보들을 외부 저장소에 수집합니다.
 */
@Component
@DependsOn("branchLoader")
@ConditionalOnProperty("elasticsearch.host")
public class BlockChainCollector implements BranchEventListener {
    private static final Logger log = LoggerFactory.getLogger(BlockChainCollector.class);

    private OutputStore outputStore;

    public BlockChainCollector(BranchGroup branchGroup, OutputStore outputStore) {
        this.outputStore = outputStore;
        for (BlockChain bc : branchGroup.getAllBranch()) {
            chainedBlock(bc.getGenesisBlock());
            bc.addListener(this);
            log.debug("ElasticSearch listener added: {}", bc.getBranch().getBranchId());
        }
    }

    @Override
    public void chainedBlock(ConsensusBlock block) {
        outputStore.put(BlockDto.createBy(block).toJsonObject());

        Map<String, JsonObject> transactionMap = new HashMap<>();

        for (Transaction tx : block.getBody().getTransactionList()) {
            EsTransactionDto dto = new EsTransactionDto(tx);
            JsonObject json = dto.toJsonObject();
            transactionMap.put(tx.getHash().toString(), json);
        }

        outputStore.put(block.getHash().toString(), block.getIndex(), transactionMap);
        log.trace("ElasticSearch put: {} [{}] tx({})",
                block.getHash().toString(), block.getIndex(), transactionMap.size());
    }

    @Override
    public void receivedTransaction(Transaction tx) {
        log.trace("ignored");
    }

    class EsTransactionDto {
        public String branchId;
        public String version;
        public String type;
        public String timestamp;
        public String bodyHash;
        public long bodyLength;
        public String signature;
        public String body;
        public String author;
        public String txId;
        public String rawTx;

        public JsonObject toJsonObject() {
            return JsonUtil.parseJsonObject(this);
        }

        public EsTransactionDto(Transaction tx) {
            TransactionBody txBody = tx.getTransactionBody();

            this.branchId = tx.getBranchId().toString();
            this.version = Hex.toHexString(tx.getHeader().getVersion());
            this.type = Hex.toHexString(tx.getHeader().getType());
            this.timestamp = Timestamps.toString(Timestamps.fromMillis(tx.getHeader().getTimestamp()));
            this.bodyHash = Hex.toHexString(txBody.getHash());
            this.bodyLength = txBody.getLength();
            this.signature = Hex.toHexString(tx.getSignature());
            this.body = txBody.toString();
            this.author = tx.getAddress().toString();
            this.txId = tx.getHash().toString();
            this.rawTx = Hex.toHexString(tx.toRawTransaction());
        }
    }
}
