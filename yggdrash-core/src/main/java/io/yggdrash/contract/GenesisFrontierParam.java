package io.yggdrash.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenesisFrontierParam extends ContractParam {

    private Map<String, Balance> frontier;

    public Map<String, Balance> getFrontier() {
        return frontier;
    }

    public void setFrontier(Map<String, Balance> frontier) {
        this.frontier = frontier;
    }

    public static class Balance {
        String balance;

        public String getBalance() {
            return balance;
        }

        public void setBalance(String balance) {
            this.balance = balance;
        }
    }
}
