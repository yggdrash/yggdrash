package io.yggdrash.core.genesis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionBody;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.core.account.Wallet;
import io.yggdrash.core.exception.NotValidateException;
import org.spongycastle.util.encoders.Hex;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GenesisBlock {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BlockInfo blockInfo;
    private final BlockHusk block;
    private String contractId;

    /**
     * Build genesis for static genesis.json
     *
     * @param branchInfoStream static generated genesis json
     */
    public GenesisBlock(InputStream branchInfoStream) {
        try {
            this.blockInfo = MAPPER.readValue(branchInfoStream, BlockInfo.class);
            this.block = new BlockHusk(toBlock().toProtoBlock());
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    /**
     * Build genesis for dynamic branch.json
     *
     * @param branchJson dynamic loaded json
     * @param wallet     for signing
     */
    GenesisBlock(BranchJson branchJson, Wallet wallet) throws Exception {
        this.blockInfo = new BlockInfo();
        this.contractId = branchJson.version;
        this.block = new BlockHusk(toBlock(branchJson, wallet).toProtoBlock());
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
        return contractId;
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

    private Block toBlock(BranchJson branchJson, Wallet wallet) throws Exception {
        JsonArray params = new JsonArray();
        params.add(Utils.parseJsonObject(branchJson.genesis));

        JsonObject genesisObject = new JsonObject();
        genesisObject.addProperty("method", "genesis");
        genesisObject.add("params", params);
        JsonArray jsonArrayTxBody = new JsonArray();
        jsonArrayTxBody.add(genesisObject);

        TransactionBody txBody = new TransactionBody(jsonArrayTxBody);

        long timestamp = ByteUtil.byteArrayToLong(Hex.decode(branchJson.timestamp));

        byte[] chain = org.spongycastle.util.encoders.Hex.decode(branchJson.branchId);

        // todo: change values(version, type) using the configuration.
        TransactionHeader txHeader = new TransactionHeader(
                chain,
                new byte[8],
                new byte[8],
                timestamp,
                txBody);

        Transaction tx = new Transaction(txHeader, wallet, txBody);
        List<Transaction> txList = new ArrayList<>();
        txList.add(tx);

        BlockBody blockBody = new BlockBody(txList);

        // todo: change values(version, type) using the configuration.
        BlockHeader blockHeader = new BlockHeader(
                chain,
                new byte[8],
                new byte[8],
                new byte[32],
                0L,
                timestamp,
                blockBody.getMerkleRoot(),
                blockBody.length());

        return new Block(blockHeader, wallet, blockBody);
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
