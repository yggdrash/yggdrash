package io.yggdrash.common.contract.vo.dpoa;

import io.yggdrash.common.contract.SerialEnum;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidatorSet implements Serializable {
    private static final long serialVersionUID = SerialEnum.VALIDATOR_SET.toValue();

    private Map<String, Validator> validatorMap;

    public ValidatorSet() {
        validatorMap = new HashMap<>();
    }

    public Map<String, Validator> getValidatorMap() {
        return validatorMap;
    }

    public void setValidatorMap(Map<String, Validator> validatorMap) {
        this.validatorMap = validatorMap;
    }

    public boolean contains(String addr) {
        return validatorMap.containsKey(addr);
    }

    public List<Validator> order(Comparator comparator) {
        List<Validator> validators = new ArrayList<>();
        if (validatorMap != null && validatorMap.size() > 0) {
            validatorMap.forEach((k, v) -> {
                if (!v.isFreezing()) {
                    validators.add(v);
                }
            });
            validators.sort(comparator == null ? Comparator.reverseOrder() : comparator);
        }

        return validators;
    }
}
