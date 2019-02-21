package io.yggdrash.gateway.controller;

import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.ContractStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
        List<ContractStatus> result = blockChain.getContractContainer().searchContracts();
        return result;
    }
}
