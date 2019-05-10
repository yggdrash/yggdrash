package io.yggdrash.contract;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.store.BranchStateStore;

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
        return true;
    }

    public void setValidators(ValidatorSet validatorSet) {
        this.set = validatorSet;
    }
}
