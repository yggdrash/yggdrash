package io.yggdrash.core.genesis;

import java.util.Map;

public class GenesisParam {

    public String operator;
    public String chainName;
    public Map<String, Balance> frontier;
    public Map<String, DelegatorInfo> delegator;
    public Map<String, NodeInfo> node;

    public static class Balance {
        public String balance;
    }

    public static class DelegatorInfo {
        public String ip;
        public String port;
    }

    public static class NodeInfo {
        public String ip;
        public String port;
    }

}
