package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.gateway.dto.BranchDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@AutoJsonRpcServiceImpl
public class BranchApiImpl implements BranchApi {

    private final BranchGroup branchGroup;

    @Autowired
    public BranchApiImpl(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @Override
    public Map<String, BranchDto> getBranches() {
        Map<String, BranchDto> branchMap = new HashMap<>();
        branchGroup.getAllBranch().forEach(blockChain ->
                branchMap.put(blockChain.getBranchId().toString(),
                        BranchDto.of(blockChain.getBranch().getJson())));
        return branchMap;
    }
}
