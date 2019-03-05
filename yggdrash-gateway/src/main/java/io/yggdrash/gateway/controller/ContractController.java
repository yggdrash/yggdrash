package io.yggdrash.gateway.controller;

import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.osgi.ContractStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    //http://localhost:8080/contract/e8b7460fce757846a438cb1d924d191ea7ebfdc6/query?contract=3-system-coin-contract-1.0.0.jar&method=balanceOf&params=%7B%22address%22%3A%22cee3d4755e47055b530deeba062c5bd0c17eb00f%22%7D
    //http://localhost:8080/contract/e8b7460fce757846a438cb1d924d191ea7ebfdc6/query?contract=2-system-dpoa-client-contract-1.0.0.jar&method=commit
    //https://www.url-encode-decode.com/
    @RequestMapping("/{branchId}/query")
    public Object query(@PathVariable(name = "branchId") String branchId, String contract, String method, @RequestParam(name = "params", required = false) String params) {
        return branchGroup.query(BranchId.of(branchId), contract, method, params != null ? JsonUtil.parseJsonObject(params) : null);
    }
}
