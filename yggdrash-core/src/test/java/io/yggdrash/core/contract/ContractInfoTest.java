package io.yggdrash.core.contract;

import io.yggdrash.common.config.DefaultConfig;
import org.junit.Ignore;
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
        Map<ContractVersion, ContractMeta> contracts = contractManager.getContracts();
        if (contracts == null) {
            return;
        }
        ContractInfo info = new ContractInfo(contractManager);
        assertEquals(contracts.entrySet().size(), info.getContracts().size());
    }

    @Test
    @Ignore
    public void getContractById() {
        Map<ContractVersion, ContractMeta> contracts = contractManager.getContracts();
        ContractInfo info = new ContractInfo(contractManager);
        if (contracts != null) {
            for (Map.Entry<ContractVersion, ContractMeta> elem : contracts.entrySet()) {
                if (elem.getKey() != null) {
                    assertEquals(elem.getValue().getMethods(),
                            info.getContractByVersion(elem.getKey()).get("methods"));
                }
            }
        }
    }

    @Test
    public void getContractIds() {
        Map<ContractVersion, ContractMeta> contracts = contractManager.getContracts();
        if (contracts == null) {
            return;
        }
        ContractInfo info = new ContractInfo(contractManager);
        assertEquals(contracts.entrySet().size(), info.getContractVersions().size());
    }
}
