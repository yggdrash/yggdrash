package io.yggdrash.core.consensus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Address;
import io.yggdrash.proto.Proto;

import java.util.Arrays;

public abstract class AbstractConsensusBlock<T extends MessageOrBuilder> implements ConsensusBlock<T> {
    private final transient Block block;

    protected AbstractConsensusBlock(Block block) {
        this.block = block;
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public Proto.Block getProtoBlock() {
        return block.getProtoBlock();
    }

    @Override
    public BlockHeader getHeader() {
        return block.getHeader();
    }

    @Override
    public byte[] getSignature() {
        return block.getSignature();
    }

    @Override
    public BlockBody getBody() {
        return block.getBody();
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
        return block.getPrevBlockHash();
    }

    @Override
    public byte[] getPubKey() {
        return block.getPubKey();
    }

    @Override
    public Address getAddress() {
        return block.getAddress();
    }

    @Override
    public long getLength() {
        return block.getLength();
    }

    @Override
    public int getSerializedSize() {
        return toBinary().length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConsensusBlock<T> other = (ConsensusBlock<T>) o;
        return Arrays.equals(toBinary(), other.toBinary());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(toBinary());
    }

    @Override
    public JsonObject toJsonObjectByProto() {
        try {
            String print = JsonFormat.printer().includingDefaultValueFields().print(getInstance());
            JsonObject jsonObject = new JsonParser().parse(print).getAsJsonObject();
            jsonObject.addProperty("blockId", getHash().toString());
            return jsonObject;
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
        }
    }

    @Override
    public int compareTo(Block o) {
        return block.compareTo(o);
    }
}
