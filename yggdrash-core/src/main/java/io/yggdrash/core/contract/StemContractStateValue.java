package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.Branch;

import java.math.BigDecimal;

/**
 * updatable branch of stem contract
 *
 */
public class StemContractStateValue extends Branch {

    private static BigDecimal fee;
    private static Long blockHeight;

    public StemContractStateValue(JsonObject json) {
        super(json);
    }

    public void init() {
        setFee(BigDecimal.ZERO);
        setBlockHeight(0L);
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
        getJson().addProperty("fee", fee);
    }

    public Long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(Long blockHeight) {
        this.blockHeight = blockHeight;
        getJson().addProperty("blockHeight", blockHeight);
    }

    public void updateValidatorSet(String validator) {
        if (getJson().has("updateValidators")) {
            //TODO if validator array
            JsonArray v = getJson().get("updateValidators").getAsJsonArray();
            v.add(validator);
        } else {
            JsonArray updateValidator = new JsonArray();
            updateValidator.add(validator);
            getJson().add("updateValidators", updateValidator);
        }
    }

    public void updateContract() {

    }

    public static StemContractStateValue of(JsonObject json) {
        return new StemContractStateValue(json.deepCopy());
    }

}