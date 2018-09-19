package io.yggdrash.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.core.Address;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionBody;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.TransactionSignature;
import io.yggdrash.core.Wallet;
import io.yggdrash.util.TimeUtils;

public class ContractTx {

    public static TransactionHusk createStemTxBySeed(
            Wallet wallet, JsonObject seed, String method) {
        JsonArray versionHistory = new JsonArray();
        versionHistory.add(seed.get("version").getAsString());
        if (!seed.has("owner")) {
            seed.addProperty("owner", wallet.getHexAddress());
        }
        if (!seed.has("timestamp")) {
            seed.addProperty("timestamp", System.currentTimeMillis());
        }
        seed.add("version_history", versionHistory);

        BranchId branchId = BranchId.of(seed);

        return createTx(wallet, BranchId.stem(), createStemTxBody(branchId, seed, method));
    }

    public static TransactionHusk createStemTxByBranch(
            Wallet wallet, JsonObject branch, String method) {
        BranchId branchId = BranchId.of(branch);

        return createTx(wallet, BranchId.stem(), createStemTxBody(branchId, branch, method));
    }


    public static JsonObject createBranch(JsonObject branch, String owner) {
        JsonArray versionHistory = new JsonArray();
        versionHistory.add(branch.get("version").getAsString());
        branch.addProperty("owner", owner);
        branch.addProperty("timestamp", System.currentTimeMillis());
        branch.add("version_history", versionHistory);

        return branch;
    }

    public static TransactionHusk createYeedTx(Wallet wallet, Address to, long amount) {
        return createTx(wallet, BranchId.yeed(), createYeedTxBody(to, amount));
    }

    private static TransactionHusk createTx(Wallet wallet, BranchId txBranchId, JsonArray body) {
        return new TransactionHusk(txBodyJson(wallet, txBranchId, body));
    }

    public static JsonArray createStemParams(BranchId branchId, JsonObject branch) {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("branchId", branchId.toString());
        param.add("branch", branch);
        params.add(param);

        return params;
    }

    private static JsonArray createStemTxBody(BranchId branchId, JsonObject branch, String method) {
        return txBodyJson(createStemParams(branchId, branch), method);
    }

    private static JsonArray createYeedTxBody(Address to, long amount) {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("address", to.toString());
        param.addProperty("amount", amount);
        params.add(param);

        return txBodyJson(params, "transfer");
    }

    private static JsonArray txBodyJson(JsonArray params, String method) {
        JsonObject txObj = new JsonObject();
        txObj.addProperty("method", method);
        txObj.add("params", params);

        JsonArray txBody = new JsonArray();
        txBody.add(txObj);

        return txBody;
    }

    private static JsonObject txBodyJson(Wallet wallet, BranchId txBranchId, JsonArray body) {

        TransactionSignature txSig;
        Transaction tx;

        TransactionBody txBody;
        txBody = new TransactionBody(body);

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
