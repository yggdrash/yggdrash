package io.yggdrash.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionBody;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.TransactionSignature;
import io.yggdrash.core.Wallet;
import io.yggdrash.util.TimeUtils;

public class ContractTx {

    public static TransactionHusk createTx(Wallet wallet, BranchId txBranchId, JsonObject body) {
        return new TransactionHusk(txBodyJson(wallet, txBranchId, body));
    }

    public static TransactionHusk createTxBySeed(Wallet wallet, BranchId txBranchId,
                                                 JsonObject branch, String method) {
        JsonArray versionHistory = new JsonArray();
        versionHistory.add(branch.get("version").getAsString());
        branch.addProperty("owner", wallet.getHexAddress());
        branch.addProperty("timestamp", System.currentTimeMillis());
        branch.add("version_history", versionHistory);

        BranchId branchId = BranchId.of(branch);

        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("branchId", branchId.toString());
        param.add("branch", branch);
        params.add(param);

        JsonObject txObj = new JsonObject();
        txObj.addProperty("method", method);
        txObj.add("params", params);

        return createTx(wallet, txBranchId, txObj);
    }

    private static JsonObject txBodyJson(Wallet wallet, BranchId txBranchId, JsonObject body) {

        TransactionSignature txSig;
        Transaction tx;

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(body);

        TransactionBody txBody;
        txBody = new TransactionBody(jsonArray);

        byte[] chain = txBranchId.getBytes();
        byte[] version = new byte[8];
        byte[] type = new byte[8];
        long timestamp = TimeUtils.time();

        TransactionHeader txHeader;
        txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);

        try {
            txSig = new TransactionSignature(wallet, txHeader.getHashForSignning());
            tx = new Transaction(txHeader, txSig, txBody);

            return tx.toJsonObject();

        } catch (Exception e) {
            return null;
        }

    }
}
