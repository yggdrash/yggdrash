package io.yggdrash.contract;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.store.BranchStateStore;
import java.util.List;

public class TestBranchStateStore implements BranchStateStore {
    ValidatorSet set = new ValidatorSet();

    @Override
    public Long getLastExecuteBlockIndex() {
        return null;
    }

    @Override
    public Sha3Hash getLastExecuteBlockHash() {
        return null;
    }

    @Override
    public Sha3Hash getGenesisBlockHash() {
        return null;
    }

    @Override
    public Sha3Hash getBranchIdHash() {
        return null;
    }

    @Override
    public ValidatorSet getValidators() {
        return set;
    }

    @Override
    public boolean isValidator(String address) {
        return set.contains(address);
    }

    @Override
    public List<BranchContract> getBranchContacts() {
        return null;
    }

    public void setValidators(ValidatorSet validatorSet) {
        this.set = validatorSet;
    }
}
