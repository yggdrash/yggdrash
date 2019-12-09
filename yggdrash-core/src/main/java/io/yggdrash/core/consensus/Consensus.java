package io.yggdrash.core.consensus;

import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.Transaction;

public class Consensus {
    private final String algorithm;
    private final String period;

    public Consensus(JsonObject consensus) {
        algorithm = consensus.get("algorithm").getAsString();
        period = consensus.get("period").getAsString();
    }

    public Consensus(Block genesisBlock) {
        this((((Transaction) genesisBlock.getBody().getTransactionList().toArray()[0])
                .getTransactionBody().getBody()).getAsJsonObject("consensus"));
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getPeriod() {
        return period;
    }
}
