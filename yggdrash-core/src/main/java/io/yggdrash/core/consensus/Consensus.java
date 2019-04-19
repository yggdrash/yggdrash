package io.yggdrash.core.consensus;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.Transaction;

import java.util.ArrayList;
import java.util.List;

public class Consensus {
    private String algorithm;
    private String period;
    private List<String> validators = new ArrayList<>();

    public Consensus(JsonObject consensus) {
        algorithm = consensus.get("algorithm").getAsString();
        period = consensus.get("period").getAsString();

        for (JsonElement validator : consensus.get("validator").getAsJsonArray()) {
            validators.add(validator.getAsString());
        }
    }

    @Deprecated
    public Consensus(Block genesisBlock) {
        this(((JsonObject) ((Transaction) genesisBlock.getBody().getBody().toArray()[0])
                .getBody().getBody().get(0)).getAsJsonObject("consensus"));
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public List<String> getValidators() {
        return validators;
    }

    public void setValidators(List<String> validators) {
        this.validators = validators;
    }
}
