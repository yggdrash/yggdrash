package io.yggdrash.gateway.controller;

import com.google.gson.JsonObject;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.osgi.ContractStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("contract")
public class ContractController {

    private BranchGroup branchGroup;

    @Autowired
    public ContractController(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @GetMapping("/{branchId}/search")
    public List<ContractStatus> search(@PathVariable(name = "branchId") String branchId) {
        BlockChain blockChain = branchGroup.getBranch(BranchId.of(branchId));
        if (blockChain == null) {
            return Collections.emptyList();
        }
        return blockChain.getContractContainer().getContractManager().searchContracts();
    }

    @GetMapping("/{branchId}/query")
    public Object query(@PathVariable(name = "branchId") String branchId, String contract,
                        String method, @RequestParam(name = "params", required = false) String params) {
        JsonObject jsonParam = params != null ? JsonUtil.parseJsonObject(params) : null;
        return branchGroup.query(BranchId.of(branchId), contract, method, jsonParam);
    }
}
