/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.core.consensus;

import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.wallet.Wallet;

public interface ConsensusMessage<T> {
    String getType();

    long getViewNumber();

    long getSeqNumber();

    byte[] getHash();

    String getHashHex();

    byte[] getHashForSigning();

    byte[] getResult();

    byte[] getSignature();

    String getSignatureHex();

    byte[] getAddress();

    String getAddressHex();

    Block getBlock();

    byte[] toBinary();

    byte[] sign(Wallet wallet);

    JsonObject toJsonObject();

    //List<T> toMessageList();

    T clone();
}