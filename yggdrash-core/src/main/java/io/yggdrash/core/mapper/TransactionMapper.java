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

package io.yggdrash.core.mapper;

import com.google.protobuf.ByteString;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.proto.BlockChainProto;

import java.io.IOException;

/**
 * The Mapper for the transaction and proto transaction.
 */
public class TransactionMapper {

    /**
     * Proto transaction to transaction.
     *
     * @param protoTx the proto transaction
     * @return the transaction
     */
    public static Transaction protoTransactionToTransaction(BlockChainProto.Transaction protoTx) throws IOException {
        TransactionHeader header = protoHeaderToHeader(protoTx.getHeader());
        Transaction transaction = new Transaction(header, protoTx.getData());
        return transaction;
    }

    /**
     * Transaction to proto transaction.
     *
     * @param tx the transaction
     * @return the proto transaction
     */
    public static BlockChainProto.Transaction transactionToProtoTransaction(Transaction tx) {
        TransactionHeader header = tx.getHeader();
        return BlockChainProto.Transaction.newBuilder().setData(tx.getData())
                .setHeader(headerToProtoHeader(header)).build();
    }

    private static TransactionHeader protoHeaderToHeader(BlockChainProto.TransactionHeader protoHeader) throws IOException {
        TransactionHeader header = new TransactionHeader(protoHeader.getDataHash().toByteArray(),
                protoHeader.getDataSize());
        header.setTimestamp(protoHeader.getTimestamp());
        header.setSignature(protoHeader.getSignature().toByteArray());
        return header;
    }

    private static BlockChainProto.TransactionHeader headerToProtoHeader(TransactionHeader header) {
        return BlockChainProto.TransactionHeader.newBuilder()
                .setType(toByteString(header.getType())).setVersion(toByteString(header.getVersion()))
                .setDataHash(toByteString(header.getDataHash())).setTimestamp(header.getTimestamp())
                .setDataSize(header.getDataSize()).setSignature(toByteString(header.getSignature()))
                .build();
    }

    private static ByteString toByteString(byte[] bytes) {
        return ByteString.copyFrom(bytes);
    }
}
