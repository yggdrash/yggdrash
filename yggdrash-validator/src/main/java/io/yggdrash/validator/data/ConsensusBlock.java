package io.yggdrash.validator.data;

import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.ProtoHusk;

public interface ConsensusBlock<T> extends ProtoHusk<T> {

    Block getBlock();

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
