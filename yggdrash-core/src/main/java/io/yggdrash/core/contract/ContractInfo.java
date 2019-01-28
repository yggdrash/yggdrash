package io.yggdrash.core.contract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContractInfo {
    private ContractManager contractManager;

    public ContractInfo(ContractManager contractManager) {
        this.contractManager = contractManager;
    }

    public List<Map> getContracts() {
        List<Map> contractList = new ArrayList<>();
        for (Map.Entry<ContractId, ContractMeta> elem : contractManager.getContracts().entrySet()) {
            contractList.add(contractInfo(elem.getValue()));
        }
        return contractList;
    }

    public Map<String, Object> getContractById(ContractId id) {
        return contractInfo(contractManager.getContractById(id));
    }

    public List<String> getContractIds() {
        List<String> contractIdList = new ArrayList<>();
        for ( ContractId key : contractManager.getContracts().keySet() ) {
            contractIdList.add(key.toString());
        }
        return contractIdList;
    }

    private static Map<String, Object> contractInfo(ContractMeta contractMeta) {
        Map<String, Object> contractInfo = new HashMap<>();
        contractInfo.put("contractId", contractMeta.getContractId().toString());
        contractInfo.put("name", contractMeta.getContract().getSimpleName());
        contractInfo.put("methods", contractMeta.getMethods());
        return contractInfo;
    }

}
