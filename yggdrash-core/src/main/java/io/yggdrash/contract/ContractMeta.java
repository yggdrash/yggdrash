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

package io.yggdrash.contract;

import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;

public class ContractMeta {
    private final Class<? extends Contract> contract;
    private final byte[] contractBinary;
    private final String contractClassName;
    private final ContractId contractId;

    ContractMeta(byte[] contractBinary, Class<? extends Contract> contractClass) {
        this.contractBinary = contractBinary;
        this.contract = contractClass;
        this.contractClassName = contractClass.getName();
        this.contractId = ContractId.of(contractBinary);
    }

    public Contract newInstance(StateStore store, TransactionReceiptStore txReceiptStore)
            throws IllegalAccessException, InstantiationException {
        // contract init
        Contract c = contract.newInstance();
        c.init(store, txReceiptStore);
        return c;
    }

    public Class<? extends Contract> getContract() {
        return contract;
    }

    ContractId getContractId() {
        return contractId;
    }
}
