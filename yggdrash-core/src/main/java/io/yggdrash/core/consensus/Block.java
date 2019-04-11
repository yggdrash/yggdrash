package io.yggdrash.core.consensus;

import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.ProtoHusk;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.wallet.Address;
import io.yggdrash.proto.Proto;

import java.util.List;

public interface Block<T> extends ProtoHusk<T> {

    io.yggdrash.core.blockchain.Block getBlock();

    Object getConsensusMessages();

    BranchId getBranchId();

    long getIndex();

    Sha3Hash getHash();

    String getHashHex();

    Sha3Hash getPrevBlockHash();

    boolean verify();

    JsonObject toJsonObject();

    void clear();

    int getBodyCount();

    List<TransactionHusk> getBody();

    long getBodyLength();

    Proto.Block getProtoBlock();

    byte[] getSignature();

    Address getAddress();

    JsonObject toJsonObjectByProto();

}
