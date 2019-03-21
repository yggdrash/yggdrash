package io.yggdrash.contract.versioning;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContractSet implements Serializable {
    private Map<String, Contract> contractMap;

    public ContractSet() {
        contractMap = new HashMap<>();
    }

    public Map<String, Contract> getContractMap() {
        return contractMap;
    }

    public void setContractMap(Map<String, Contract> contractMap) {
        this.contractMap = contractMap;
    }

    public List<Contract> order(Comparator comparator) {
        List<Contract> contracts = new ArrayList<>();
        if (contractMap != null && contractMap.size() > 0) {
            contractMap.forEach((k, c) -> {
                if (!c.isUpgrade()) {
                    contracts.add(c);
                }
            });
            contracts.sort(comparator == null ? Comparator.reverseOrder() : comparator);
        }

        return contracts;
    }

}
