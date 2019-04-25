package io.yggdrash.core.consensus;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.Transaction;

import java.util.ArrayList;
import java.util.List;

public class Consensus {
    private final String algorithm;
    private final String period;
    private final List<String> validators = new ArrayList<>();

    public Consensus(JsonObject consensus) {
        algorithm = consensus.get("algorithm").getAsString();
        period = consensus.get("period").getAsString();

        for (JsonElement validator : consensus.get("validator").getAsJsonArray()) {
            validators.add(validator.getAsString());
        }
    }

    @Deprecated
    public Consensus(Block genesisBlock) {
        this((((Transaction) genesisBlock.getBody().getTransactionList().toArray()[0])
                .getBody().getBody()).getAsJsonObject("consensus"));
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getPeriod() {
        return period;
    }

    public List<String> getValidators() {
        return validators;
    }
}
