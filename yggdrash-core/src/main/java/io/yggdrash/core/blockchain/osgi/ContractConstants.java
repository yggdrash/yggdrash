package io.yggdrash.core.blockchain.osgi;

public class ContractConstants {
    private ContractConstants() {
        throw new IllegalStateException("Contract Constants class");
    }

    // Bundle location prefix
    public static final String SUFFIX_SYSTEM_CONTRACT = "contract/system";
    public static final String SUFFIX_USER_CONTRACT = "contract/user";

    // Executor Type
    public static final String BUNDLE_CONTRACT = "0000000000000000";
    public static final String VERSIONING_CONTRACT = "0000000000000001";

}
