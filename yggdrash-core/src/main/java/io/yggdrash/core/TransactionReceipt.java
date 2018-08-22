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

package io.yggdrash.core;

import java.util.HashMap;
import java.util.Map;

public class TransactionReceipt {
    public String transactionHash =
            "0xb903239f8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238";
    public int transactionIndex = 1;
    public String blockHash =
            "0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b";
    public int yeedUsed = 30000;
    public String branchAddress =
            "0xb60e8dd61c5d32be8058bb8eb970870f07233155";
    public Map<String,String> txLog = new HashMap<>();
    public int status = 1;

    public TransactionReceipt() {

    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    @Override
    public String toString() {
        return "TransactionReceipt{"
                + "transactionHash='" + transactionHash + '\''
                + ", transactionIndex=" + transactionIndex
                + ", blockHash='" + blockHash + '\''
                + ", yeedUsed=" + yeedUsed
                + ", branchAddress='" + branchAddress + '\''
                + ", txLog=" + txLog
                + ", status=" + status
                + '}';
    }
}
