package io.yggdrash.node.mock;

import com.google.gson.JsonObject;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;

import java.io.IOException;

public class TransactionMock {

    private final NodeManager nodeManager;

    public TransactionMock(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public Transaction retTxMock() throws IOException {

        // Create transaction
        JsonObject txObj = new JsonObject();

        txObj.addProperty("operator", "transfer");
        txObj.addProperty("to", "0x9843DC167956A0e5e01b3239a0CE2725c0631392");
        txObj.addProperty("value", 100);

        return new Transaction(this.nodeManager.getWallet(), txObj);
    }
}
