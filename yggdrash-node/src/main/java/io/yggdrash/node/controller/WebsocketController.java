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

package io.yggdrash.node.controller;

import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.event.BranchEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebsocketController implements BranchEventListener {

    private final SimpMessagingTemplate template;

    @Autowired
    public WebsocketController(BranchGroup branchGroup, SimpMessagingTemplate template) {
        branchGroup.getAllBranch().forEach(blockChain -> blockChain.addListener(this));
        this.template = template;
    }

    @SendTo("/topic/blocks")
    public BlockDto block(BlockDto block) {
        return block;
    }

    @SendTo("/topic/txs")
    public TransactionDto tx(TransactionDto tx) {
        return tx;
    }

    @Override
    public void chainedBlock(BlockHusk block) {
        template.convertAndSend("/topic/blocks", BlockDto.createBy(block));
    }

    @Override
    public void receivedTransaction(TransactionHusk tx) {
        template.convertAndSend("/topic/txs", TransactionDto.createBy(tx));
    }
}
