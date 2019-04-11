package io.yggdrash.core.consensus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Address;
import io.yggdrash.proto.Proto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractBlock<T extends MessageOrBuilder> implements Block<T> {
    private final T proto;
    private final transient io.yggdrash.core.blockchain.Block block;
    private transient List<TransactionHusk> body;

    protected AbstractBlock(T proto) {
        this.proto = proto;
        this.block = io.yggdrash.core.blockchain.Block.toBlock(getProtoBlock());
        initConsensus();
    }

    protected abstract void initConsensus();

    @Override
    public T getInstance() {
        return proto;
    }

    @Override
    public int getBodyCount() {
        return getProtoBlock().getBody().getTransactionsCount();
    }

    @Override
    public List<TransactionHusk> getBody() {
        if (body != null) {
            return body;
        }
        this.body = new ArrayList<>();
        for (Proto.Transaction tx : getProtoBlock().getBody().getTransactionsList()) {
            body.add(new TransactionHusk(tx));
        }
        return body;
    }

    @Override
    public long getBodyLength() {
        return getProtoBlock().getBody().getTransactionsCount();
    }

    @Override
    public byte[] getSignature() {
        return getProtoBlock().getSignature().toByteArray();
    }

    @Override
    public Address getAddress() {
        return new Address(block.getAddress());
    }

    @Override
    public io.yggdrash.core.blockchain.Block getBlock() {
        return block;
    }

    @Override
    public BranchId getBranchId() {
        return BranchId.of(block.getChain());
    }

    @Override
    public long getIndex() {
        return this.block.getIndex();
    }

    @Override
    public Sha3Hash getHash() {
        return Sha3Hash.createByHashed(block.getHash());
    }

    @Override
    public String getHashHex() {
        return this.block.getHashHex();
    }

    @Override
    public Sha3Hash getPrevBlockHash() {
        return Sha3Hash.createByHashed(block.getPrevBlockHash());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Block<T> that = (Block<T>) o;
        return Arrays.equals(getData(), that.getData());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getData());
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
            String print = JsonFormat.printer()
                    .includingDefaultValueFields().print(proto);
            JsonObject jsonObject = new JsonParser().parse(print).getAsJsonObject();
            jsonObject.addProperty("blockId", getHash().toString());
            return jsonObject;
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
        }
    }

}
