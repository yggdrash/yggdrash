package io.yggdrash.contract.versioning;

import com.google.gson.JsonObject;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;

/**
 * updatable contract version
 *
 */
public class VersioningContractStateValue {

    private static String targetContract;
    private static Long blockHeight;
    private static ValidatorSet validatorSet;

    public VersioningContractStateValue(JsonObject json) {

        if (json.has("contract")) {

        }

        if (json.has("contractVersion")) {

        }
    }

    public void updateContract() {

    }

    public void setBlockHeight(Long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public static VersioningContractStateValue of(JsonObject json) {
        return new VersioningContractStateValue(json.deepCopy());
    }

}
