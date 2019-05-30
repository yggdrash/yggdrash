package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.gateway.dto.BranchDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Override
    public Set<String> getValidators(String branchId) {
        BlockChain bc = branchGroup.getBranch(BranchId.of(branchId));
        if (bc != null) {
            ValidatorSet vs = bc.getValidators();
            return vs.getValidatorMap().keySet();
        } else {
            return null;
        }
    }


}
