package io.yggdrash.core.consensus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
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

public abstract class AbstractBlock<T> implements Block<T> {
    private final Proto.Block protoBlock;
    private final transient io.yggdrash.core.blockchain.Block block;
    private transient List<TransactionHusk> body;

    protected AbstractBlock(Proto.Block protoBlock) {
        this.protoBlock = protoBlock;
        this.block = io.yggdrash.core.blockchain.Block.toBlock(getProtoBlock());
    }

    @Override
    public Proto.Block getProtoBlock() {
        return protoBlock;
    }

    @Override
    public int getBodyCount() {
        return protoBlock.getBody().getTransactionsCount();
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
        return protoBlock.getBody().getTransactionsCount();
    }

    @Override
    public byte[] getSignature() {
        return protoBlock.getSignature().toByteArray();
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
        return BranchId.of(protoBlock.getHeader().getChain().toByteArray());
    }

    @Override
    public long getIndex() {
        return this.protoBlock.getHeader().getIndex();
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
        return Sha3Hash.createByHashed(protoBlock.getHeader().getPrevBlockHash().toByteArray());
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
                    .includingDefaultValueFields().print(protoBlock);
            JsonObject jsonObject = new JsonParser().parse(print).getAsJsonObject();
            jsonObject.addProperty("blockId", getHash().toString());
            return jsonObject;
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
        }
    }

}
