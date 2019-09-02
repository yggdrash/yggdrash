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

package io.yggdrash.gateway;

import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchEventListener;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.gateway.dto.BlockDto;
import io.yggdrash.gateway.dto.TransactionDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebsocketSender implements BranchEventListener {
    private final SimpMessagingTemplate template;

    public WebsocketSender(SimpMessagingTemplate template, BranchGroup branchGroup) {
        this.template = template;
        for (BlockChain bc : branchGroup.getAllBranch()) {
            bc.addListener(this);
        }
    }

    @Override
    public void chainedBlock(ConsensusBlock block) {
        BranchId branchId = block.getBranchId();
        template.convertAndSend("/topic/blocks", BlockDto.createBy(block));
        template.convertAndSend("/topic/branches/" + branchId + "/blocks",
                BlockDto.createBy(block));
    }

    @Override
    public void receivedTransaction(Transaction tx) {
        BranchId branchId = tx.getBranchId();
        template.convertAndSend("/topic/txs", TransactionDto.createBy(tx));
        template.convertAndSend("/topic/branches/" + branchId + "/txs",
                TransactionDto.createBy(tx));
    }
}
