package io.yggdrash.core.genesis;

import java.util.List;
import java.util.Map;

class GenesisBody {

    public String method;
    public String branchId;
    public String contractId;
    public List<Map<String, String>> params;
    public Map<String, DelegatorInfo> delegator;
    public Map<String, NodeInfo> node;

    public static class DelegatorInfo {
        public String ip;
        public String port;
    }

    public static class NodeInfo {
        public String ip;
        public String port;
    }

}
