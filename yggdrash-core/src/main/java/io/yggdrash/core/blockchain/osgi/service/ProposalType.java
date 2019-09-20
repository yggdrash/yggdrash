package io.yggdrash.core.blockchain.osgi.service;

import java.util.HashMap;
import java.util.Map;

public enum ProposalType {
    ACTIVATE,
    DEACTIVATE;

    private static final Map<String, ProposalType> string2Enum = new HashMap<String, ProposalType>();

    static {
        for (ProposalType type: values()) {
            string2Enum.put(type.name(), type);
        }
    }

    public static ProposalType findBy(String type) {
        return string2Enum.get(type);
    }

}
