package io.yggdrash.core.contract;

import io.yggdrash.common.config.DefaultConfig;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ContractInfoTest {
    Logger log = LoggerFactory.getLogger(ContractManagerTest.class);

    private static DefaultConfig defaultConfig = new DefaultConfig();
    private static ContractManager contractManager = new ContractManager(defaultConfig.getContractPath());

    @Test
    public void getContracts() {
        Map<ContractId, ContractMeta> contracts = contractManager.getContracts();
        if (contracts == null) return;
        ContractInfo info = new ContractInfo(contractManager);
        assertEquals(contracts.entrySet().size(), info.getContracts().size());
    }

    @Test
    public void getContractById() {
        Map<ContractId, ContractMeta> contracts = contractManager.getContracts();
        ContractInfo info = new ContractInfo(contractManager);
        if (contracts != null) {
            for (Map.Entry<ContractId, ContractMeta> elem : contracts.entrySet()) {
                if (elem.getKey() != null){
                    assertEquals(elem.getValue().getMethods(), info.getContractById(elem.getKey()).get("methods"));
                }
            }
        }
    }

    @Test
    public void getContractIds() {
        Map<ContractId, ContractMeta> contracts = contractManager.getContracts();
        if (contracts == null) return;
        ContractInfo info = new ContractInfo(contractManager);
        assertEquals(contracts.entrySet().size(), info.getContractIds().size());
    }
}
