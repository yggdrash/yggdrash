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

import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchEventListener;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.node.controller.BlockDto;
import io.yggdrash.node.controller.TransactionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebsocketSender implements BranchEventListener {

    private final SimpMessagingTemplate template;
    private BranchId stemBranchId = BranchId.NULL;

    @Autowired
    public WebsocketSender(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void setStemBranchId(BranchId stemBranchId) {
        this.stemBranchId = stemBranchId;
    }

    @Override
    public void chainedBlock(BlockHusk block) {
        BranchId branchId = block.getBranchId();
        template.convertAndSend("/topic/blocks", BlockDto.createBy(block));
        template.convertAndSend("/topic/branches/" + branchId + "/blocks",
                BlockDto.createBy(block));
        if (block.getBranchId().equals(stemBranchId)) {
            template.convertAndSend("/topic/stem/blocks", BlockDto.createBy(block));
        }
    }

    @Override
    public void receivedTransaction(TransactionHusk tx) {
        BranchId branchId = tx.getBranchId();
        template.convertAndSend("/topic/txs", TransactionDto.createBy(tx));
        template.convertAndSend("/topic/branches/" + branchId + "/txs",
                TransactionDto.createBy(tx));
        if (tx.getBranchId().equals(stemBranchId)) {
            template.convertAndSend("/topic/stem/txs", TransactionDto.createBy(tx));
        }
    }
}
