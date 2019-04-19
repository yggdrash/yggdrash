package io.yggdrash.core.consensus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.exception.NotValidateException;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractBlock<T> implements Block<T> {
    private final transient io.yggdrash.core.blockchain.Block block;

    protected AbstractBlock(io.yggdrash.core.blockchain.Block block) {
        this.block = block;
    }

    @Override
    public int getBodyCount() {
        return block.getBody().getCount();
    }

    @Override
    public List<Transaction> getTransactionList() {
        return block.getBody().getTransactionList();
    }

    @Override
    public long getBodyLength() {
        return block.getHeader().getBodyLength();
    }

    @Override
    public byte[] getSignature() {
        return block.getSignature();
    }

    @Override
    public io.yggdrash.core.blockchain.Block getBlock() {
        return block;
    }

    @Override
    public BranchId getBranchId() {
        return block.getBranchId();
    }

    @Override
    public long getIndex() {
        return this.block.getHeader().getIndex();
    }

    @Override
    public Sha3Hash getHash() {
        return block.getHash();
    }

    @Override
    public Sha3Hash getPrevBlockHash() {
        return Sha3Hash.createByHashed(block.getHeader().getPrevBlockHash());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Block<T> other = (Block<T>) o;
        return Arrays.equals(toBinary(), other.toBinary());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(toBinary());
    }

    @Override
    public boolean verify() {
        if (this.block == null) {
            return false;
        }

        // todo: check consensuses whether validator's signatures or not
        return this.block.verify();
    }

    @Override
    public JsonObject toJsonObjectByProto() {
        try {
            String print = JsonFormat.printer().includingDefaultValueFields().print(block.getInstance());
            JsonObject jsonObject = new JsonParser().parse(print).getAsJsonObject();
            jsonObject.addProperty("blockId", getHash().toString());
            return jsonObject;
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
        }
    }

}
