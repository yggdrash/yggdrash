/*
 * Copyright 2019 Akashic Foundation
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

public interface Block extends Comparable<Block> {

    Proto.Block getProtoBlock();

    BlockHeader getHeader();

    byte[] getSignature();

    BlockBody getBody();

    BranchId getBranchId();

    long getIndex();

    /**
     * Get block hash. SHA3(header | signature)
     *
     * @return block hash
     */
    Sha3Hash getHash();

    Sha3Hash getPrevBlockHash();

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
     * Get the Block length (Header + Signature + Body).
     *
     * @return block length
     */
    long getLength();

    JsonObject toJsonObject();

    void clear();
}
