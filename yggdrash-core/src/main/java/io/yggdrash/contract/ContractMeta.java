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
import io.yggdrash.crypto.HashUtil;
import java.nio.ByteBuffer;

public class ContractMeta {
    Class<Contract> contract;
    byte[] contractBinaly;
    ByteBuffer contractId;

    public ContractMeta(byte[] contractBinaly, Class<Contract> contractClass) {
        this.contractBinaly = contractBinaly;
        this.contract = contractClass;
        this.contractId = ByteBuffer.wrap(ContractMeta.convertId(contractBinaly));
    }

    public Contract newInstance(StateStore store, TransactionReceiptStore txReceiptStore)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException {
        // contract init
        Contract c = contract.newInstance();
        c.init(store, txReceiptStore);
        return c;
    }

    public Class<Contract> getContract() {
        return this.contract;
    }

    public ByteBuffer getContractId() {
        return this.contractId;
    }

    public static byte[] convertId(byte[] contractBytes) {
        return HashUtil.sha1(contractBytes);
    }

}
