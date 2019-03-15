package io.yggdrash.validator.data;

import com.google.gson.JsonObject;

public interface ConsensusBlock {

    io.yggdrash.core.blockchain.Block getBlock();

    Object getConsensusMessages();

    byte[] getChain();

    long getIndex();

    byte[] getHash();

    String getHashHex();

    byte[] getPrevBlockHash();

    boolean verify();

    byte[] toBinary();

    JsonObject toJsonObject();

    boolean equals(ConsensusBlock consensusBlock);

    ConsensusBlock clone();

    void clear();
}
