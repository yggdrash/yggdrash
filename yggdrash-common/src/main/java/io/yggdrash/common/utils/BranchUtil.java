package io.yggdrash.common.utils;

import com.google.gson.JsonObject;
import io.yggdrash.common.crypto.HashUtil;

import static io.yggdrash.common.utils.SerializationUtil.serializeString;

public class BranchUtil {

    public static byte[] branchIdGenerator(JsonObject jsonObject) {
        byte[] branchBytes = serializeString(jsonObject.toString());
        return BranchUtil.branchIdGenerator(branchBytes);
    }

    public static byte[] branchIdGenerator(byte[] branchBytes) {
        return HashUtil.sha3omit12(branchBytes);
    }

}
