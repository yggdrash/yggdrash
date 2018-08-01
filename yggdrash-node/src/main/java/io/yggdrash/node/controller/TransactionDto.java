/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.controller;

import com.google.gson.JsonObject;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.Wallet;
import io.yggdrash.node.MessageSender;
import io.yggdrash.node.config.NodeProperties;
import io.yggdrash.node.mock.NodeManagerMock;

import java.io.IOException;
import java.security.SignatureException;

public class TransactionDto {
    private static final NodeManager nodeManager = new NodeManagerMock(new MessageSender(), null,
            new NodeProperties.Grpc());
    private String from;
    private String txHash;
    private String data;

    public static Transaction of(TransactionDto transactionDto) throws IOException {
        Wallet wallet = nodeManager.getWallet();
        JsonObject jsonData = new JsonObject();
        jsonData.addProperty("data", transactionDto.getData());
        return new Transaction(wallet, jsonData);
    }

    public static TransactionDto createBy(Transaction tx)
            throws IOException, SignatureException {
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setFrom(tx.getHeader().getAddressToString());
        transactionDto.setData(tx.getData());
        transactionDto.setTxHash(tx.getHashString());
        return transactionDto;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }
}
