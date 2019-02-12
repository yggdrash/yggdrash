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
        for (Map.Entry<ContractVersion, ContractMeta> elem : contractManager.getContracts().entrySet()) {
            contractList.add(contractInfo(elem.getValue()));
        }
        return contractList;
    }

    public Map<String, Object> getContractByVersion(ContractVersion version) {
        return contractInfo(contractManager.getContractByVersion(version));
    }

    public List<String> getContractVersions() {
        List<String> contractIdList = new ArrayList<>();
        for (ContractVersion key : contractManager.getContracts().keySet()) {
            contractIdList.add(key.toString());
        }
        return contractIdList;
    }

    private static Map<String, Object> contractInfo(ContractMeta contractMeta) {
        Map<String, Object> contractInfo = new HashMap<>();
        contractInfo.put("contractId", contractMeta.getContractVersion().toString());
        contractInfo.put("name", contractMeta.getContract().getSimpleName());
        contractInfo.put("methods", contractMeta.getMethods());
        return contractInfo;
    }

}
