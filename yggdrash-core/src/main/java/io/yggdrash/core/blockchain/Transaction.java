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

package io.yggdrash.core.blockchain;

import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.wallet.Address;
import io.yggdrash.proto.Proto;

public interface Transaction extends ProtoObject<Proto.Transaction>, Comparable<Transaction> {

    /**
     * Get TransactionHeader.
     *
     * @return transaction header class
     */
    TransactionHeader getHeader();

    /**
     * Get Transaction signature.
     *
     * @return transaction signature
     */
    byte[] getSignature();

    /**
     * Get TransactionBody.
     *
     * @return transaction body class
     */
    TransactionBody getTransactionBody();

    BranchId getBranchId();

    /**
     * Get transaction hash. SHA3(header | signature)
     *
     * @return transaction hash
     */
    Sha3Hash getHash();

    /**
     * Get the public key.
     *
     * @return public key
     */
    byte[] getPubKey();

    /**
     * Get the address.
     *
     * @return address
     */
    Address getAddress();

    /**
     * Get the Transaction length (Header + Signature + Body).
     *
     * @return tx length
     */
    long getLength();

    /**
     * Get the Proto Transaction
     *
     * @return proto tx
     */
    Proto.Transaction getProtoTransaction();

    /**
     * Convert from Transaction.class to JsonObject.
     *
     * @return transaction as JsonObject
     */
    JsonObject toJsonObject();

    JsonObject toJsonObjectFromProto();

    byte[] toRawTransaction();
}
