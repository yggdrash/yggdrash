package io.yggdrash.core.consensus;

import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.ProtoObject;
import io.yggdrash.core.blockchain.Transaction;

import java.util.List;

public interface Block<T> extends ProtoObject<T> {

    io.yggdrash.core.blockchain.Block getBlock();

    Object getConsensusMessages();

    BranchId getBranchId();

    long getIndex();

    Sha3Hash getHash();

    Sha3Hash getPrevBlockHash();

    boolean verify();

    JsonObject toJsonObject();

    void clear();

    int getBodyCount();

    List<Transaction> getTransactionList();

    long getBodyLength();

    byte[] getSignature();

    JsonObject toJsonObjectByProto();

}
