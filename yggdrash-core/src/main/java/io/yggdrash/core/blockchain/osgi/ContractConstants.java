package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.crypto.HashUtil;
import org.apache.commons.codec.binary.Hex;

public class ContractConstants {

    private ContractConstants() {
        throw new IllegalStateException("Contract Constants class");
    }

    // Bundle location prefix
    public static final String SUFFIX_SYSTEM_CONTRACT = "contract/system";
    public static final String SUFFIX_USER_CONTRACT = "contract/user";

    // Executor Type
    public static final String BUNDLE_TRANSACTION = "0000000000000000";
    public static final String VERSIONING_TRANSACTION = "0000000000000001";

    public static final ContractVersion VERSIONING_CONTRACT
            = ContractVersion.of(Hex.encodeHexString(HashUtil.sha3omit12("VersioningContract".getBytes())));

}
