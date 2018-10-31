package io.yggdrash.core.genesis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionBody;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.core.exception.NotValidateException;
import org.spongycastle.util.encoders.Hex;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GenesisBlock {

    private final BlockInfo blockInfo;
    private final BlockHusk block;

    public GenesisBlock(InputStream branchInfoStream) {
        try {
            this.blockInfo = new ObjectMapper().readValue(branchInfoStream, BlockInfo.class);
            this.block = new BlockHusk(toBlock().toProtoBlock());
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    public BlockHusk getBlock() {
        return block;
    }

    public String getContractId() {
        if (blockInfo.body != null && !blockInfo.body.isEmpty()) {
            TransactionInfo txInfo = blockInfo.body.get(0);
            if (txInfo.body != null && !txInfo.body.isEmpty()) {
                return txInfo.body.get(0).contractId;
            }
        }
        return null;
    }

    private Block toBlock() {
        BlockHeader blockHeader = new BlockHeader(
                Hex.decode(blockInfo.header.chain),
                Hex.decode(blockInfo.header.version),
                Hex.decode(blockInfo.header.type),
                Hex.decode(blockInfo.header.prevBlockHash),
                ByteUtil.byteArrayToLong(Hex.decode(blockInfo.header.index)),
                ByteUtil.byteArrayToLong(Hex.decode(blockInfo.header.timestamp)),
                Hex.decode(blockInfo.header.merkleRoot),
                ByteUtil.byteArrayToLong(Hex.decode(blockInfo.header.bodyLength))
        );

        List<Transaction> txList = new ArrayList<>();

        for (TransactionInfo txi : blockInfo.body) {
            txList.add(toTransaction(txi));
        }

        BlockBody txBody = new BlockBody(txList);

        return new Block(blockHeader, Hex.decode(blockInfo.signature), txBody);
    }

    private Transaction toTransaction(TransactionInfo txi) {

        TransactionHeader txHeader = new TransactionHeader(
                Hex.decode(txi.header.chain),
                Hex.decode(txi.header.version),
                Hex.decode(txi.header.type),
                ByteUtil.byteArrayToLong(Hex.decode(txi.header.timestamp)),
                Hex.decode(txi.header.bodyHash),
                ByteUtil.byteArrayToLong(Hex.decode(txi.header.bodyLength))
        );

        TransactionBody txBody = new TransactionBody(new Gson().toJson(txi.body));

        return new Transaction(txHeader, Hex.decode(txi.signature), txBody);
    }
}
