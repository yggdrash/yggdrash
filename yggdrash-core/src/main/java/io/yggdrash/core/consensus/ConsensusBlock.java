package io.yggdrash.core.consensus;

import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.ProtoObject;

public interface ConsensusBlock<T> extends Block, ProtoObject<T> {

    Block getBlock();

    Object getConsensusMessages();

    JsonObject getConsensusMessagesJsonObject();

    JsonObject toJsonObjectByProto();

    int getSerializedSize();

    void loggingBlock(int unConfirmedTxs);
}
