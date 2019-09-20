/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.blockchain;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Address;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.Proto;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;

import static io.yggdrash.common.config.Constants.EMPTY_BYTE8;
import static io.yggdrash.common.config.Constants.Key.BODY;
import static io.yggdrash.common.config.Constants.Key.HEADER;
import static io.yggdrash.common.config.Constants.Key.SIGNATURE;

public class BlockImpl implements Block, ProtoObject<Proto.Block> {

    private final Proto.Block protoBlock;

    private final transient BlockHeader header;
    private final transient BlockBody body;
    private transient Sha3Hash hash;
    private transient Address address;

    /**
     * Block Constructor.
     *
     * @param bytes binary block
     */
    public BlockImpl(byte[] bytes) {
        this(toProto(bytes));
    }

    public BlockImpl(Proto.Block protoBlock) {
        this.protoBlock = protoBlock;
        this.header = new BlockHeader(protoBlock.getHeader());
        this.body = new BlockBody(protoBlock.getBody());
    }

    /**
     * Block Constructor.
     *
     * @param header block header
     * @param wallet wallet for signing
     * @param body   block body
     */
    public BlockImpl(BlockHeader header, Wallet wallet, BlockBody body) {
        this(header, wallet.sign(header.getHashForSigning(), true), body);
    }

    /**
     * Block Constructor.
     *
     * @param header    block header
     * @param signature block signature
     * @param body      block body
     */
    public BlockImpl(BlockHeader header, byte[] signature, BlockBody body) {
        this.header = header;
        this.body = body;
        this.protoBlock = Proto.Block.newBuilder()
                .setHeader(header.getInstance())
                .setSignature(ByteString.copyFrom(signature))
                .setBody(body.getInstance())
                .build();
    }

    /**
     * Block Constructor.
     *
     * @param jsonObject jsonObject block
     */
    public BlockImpl(JsonObject jsonObject) {
        this(new BlockHeader(jsonObject.getAsJsonObject(HEADER)),
                Hex.decode(jsonObject.get(SIGNATURE).getAsString()),
                new BlockBody(jsonObject.getAsJsonArray(BODY)));
    }

    @Override
    public Proto.Block getProtoBlock() {
        return protoBlock;
    }

    @Override
    public BlockHeader getHeader() {
        return header;
    }

    @Override
    public byte[] getSignature() {
        return protoBlock.getSignature().toByteArray();
    }

    @Override
    public BlockBody getBody() {
        return body;
    }

    @Override
    public BranchId getBranchId() {
        return BranchId.of(header.getChain());
    }

    @Override
    public Sha3Hash getPrevBlockHash() {
        return Sha3Hash.createByHashed(header.getPrevBlockHash());
    }

    @Override
    public long getIndex() {
        return header.getIndex();
    }

    @Override
    public Sha3Hash getHash() {
        if (hash == null) {
            setHash();
        }
        return hash;
    }

    private void setHash() {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        try {
            bao.write(header.getBinaryForSigning());
            bao.write(getSignature());
        } catch (IOException e) {
            throw new NotValidateException();
        }
        this.hash = new Sha3Hash(bao.toByteArray());
    }

    @Override
    public byte[] getPubKey() {
        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(getSignature());
        try {
            ECKey ecKeyPub = ECKey.signatureToKey(this.header.getHashForSigning(), ecdsaSignature);
            return ecKeyPub.getPubKey();
        } catch (SignatureException e) {
            throw new InvalidSignatureException(e);
        }
    }

    @Override
    public Address getAddress() {
        if (address == null) {
            setAddress();
        }
        return address;
    }

    private void setAddress() {
        try {
            this.address = new Address(getPubKey());
        } catch (Exception e) {
            this.address = Address.NULL_ADDRESS;
        }
    }

    @Override
    public long getLength() {
        return BlockHeader.LENGTH + Constants.SIGNATURE_LENGTH + header.getBodyLength();
    }

    @Override
    public byte[] toBinary() {
        return protoBlock.toByteArray();
    }

    @Override
    public Proto.Block getInstance() {
        return getProtoBlock();
    }

    @Override
    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.add(HEADER, this.header.toJsonObject());
        jsonObject.addProperty(SIGNATURE, Hex.toHexString(getSignature()));
        jsonObject.add(BODY, this.body.toJsonArray());

        return jsonObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BlockImpl other = (BlockImpl) o;
        return Arrays.equals(toBinary(), other.toBinary());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(toBinary());
    }

    @Override
    public String toString() {
        return toJsonObject().toString();
    }

    @Override
    public int compareTo(Block o) {
        return Long.compare(getIndex(), o.getIndex());
    }

    @Override
    public void clear() {
        body.getTransactionList().clear();
    }

    private static Proto.Block toProto(byte[] bytes) {
        try {
            return Proto.Block.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
        }
    }

    @VisibleForTesting
    public static Block nextBlock(Wallet wallet, List<Transaction> body, byte[] stateRoot, ConsensusBlock prevBlock) {
        if (body == null || prevBlock == null) {
            throw new NotValidateException();
        }

        BlockBody blockBody = new BlockBody(body);
        BlockHeader blockHeader = new BlockHeader(
                prevBlock.getBranchId().getBytes(),
                EMPTY_BYTE8,
                EMPTY_BYTE8,
                prevBlock.getHash().getBytes(),
                prevBlock.getIndex() + 1,
                TimeUtils.time(),
                stateRoot,
                blockBody);
        return new BlockImpl(blockHeader, wallet, blockBody);
    }
}
