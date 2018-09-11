package io.yggdrash.core.genesis;

import java.util.List;
import java.util.Map;

public class GenesisBody {

    public String method;
    public String branchName;
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
