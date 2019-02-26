package io.yggdrash.gateway.controller;

import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.dpoa.Validator;
import io.yggdrash.core.blockchain.osgi.ContractManager;
import io.yggdrash.core.blockchain.osgi.ContractStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("contract")
public class ContractController {
    @Autowired
    private BranchGroup branchGroup;

    @RequestMapping("/{branchId}/search")
    public List<ContractStatus> search(@PathVariable(name = "branchId") String branchId) {
        BlockChain blockChain = branchGroup.getBranch(BranchId.of(branchId));
        if (blockChain == null) {
            return null;
        }
        List<ContractStatus> result = blockChain.getContractContainer().getContractManager().searchContracts();
        return result;
    }

    @RequestMapping("/{branchId}/invoke")
    public List<Validator> invoke(@PathVariable(name = "branchId") String branchId, String contract, String method) {
        BlockChain blockChain = branchGroup.getBranch(BranchId.of(branchId));
        if (blockChain == null) {
            return null;
        }

        ContractManager contractManager = blockChain.getContractContainer().getContractManager();
        String contractPath = contractManager.makeContractFullPath(contract, contractManager.checkSystemContract(contract));

        JsonObject txBody = new JsonObject();
        txBody.addProperty("method", "commit");
        Object result = contractManager.invoke(contractPath, txBody, null);

        return result == null ? null : (List<Validator>) result;
    }
}
