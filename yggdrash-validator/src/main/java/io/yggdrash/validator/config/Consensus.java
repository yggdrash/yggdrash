package io.yggdrash.validator.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.ArrayList;
import java.util.List;

@Configuration
@DependsOn("validatorGenesisBlock")
public class Consensus {
    private String algorithm;
    private String period;
    private List<String> validators = new ArrayList<>();

    @Autowired
    public Consensus(@Qualifier("validatorGenesisBlock") Block genesisBlock) {
        JsonObject consensusObject = ((JsonObject) ((Transaction) genesisBlock.getBody().getBody().toArray()[0])
                .getBody().getBody().get(0)).getAsJsonObject("consensus");
        algorithm = consensusObject.get("algorithm").getAsString();
        period = consensusObject.get("period").getAsString();

        for (JsonElement validator : consensusObject.get("validator").getAsJsonArray()) {
            validators.add(validator.getAsString());
        }
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
