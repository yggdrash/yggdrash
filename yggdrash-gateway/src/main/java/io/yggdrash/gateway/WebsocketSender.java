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
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.consensus.Block;
import io.yggdrash.gateway.dto.BlockDto;
import io.yggdrash.gateway.dto.TransactionDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@DependsOn("yggdrash")
@ConditionalOnProperty(name = "yggdrash.node.chain.enabled", matchIfMissing = true)
//TODO
// @ConditionalOnProperty("es.host")
public class WebsocketSender implements BranchEventListener {
    private final SimpMessagingTemplate template;

    public WebsocketSender(SimpMessagingTemplate template, BranchGroup branchGroup) {
        this.template = template;
        for (BlockChain bc : branchGroup.getAllBranch()) {
            bc.addListener(this);
        }
    }

    @Override
    public void chainedBlock(Block block) {
        BranchId branchId = block.getBranchId();
        template.convertAndSend("/topic/blocks", BlockDto.createBy(block));
        template.convertAndSend("/topic/branches/" + branchId + "/blocks",
                BlockDto.createBy(block));
    }

    @Override
    public void receivedTransaction(TransactionHusk tx) {
        BranchId branchId = tx.getBranchId();
        template.convertAndSend("/topic/txs", TransactionDto.createBy(tx));
        template.convertAndSend("/topic/branches/" + branchId + "/txs",
                TransactionDto.createBy(tx));
    }
}
