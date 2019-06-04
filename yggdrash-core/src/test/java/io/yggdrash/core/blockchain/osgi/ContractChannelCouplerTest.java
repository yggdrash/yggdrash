package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.core.contract.TestContract;
import org.junit.Before;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

public class ContractChannelCouplerTest {

    ContractCache cache;
    Map<String, Object> contractMap;

    @Before
    public void setup() {
        TestContract a = new TestContract();
        TestContract b = new TestContract();

        contractMap = new HashMap<>();
        contractMap.put("TEST1", a);
        contractMap.put("TEST2", b);

        cache = new ContractCacheImpl();

        cache.cacheContract("TEST1", a);
        cache.cacheContract("TEST2", b);

    }

    @Test
    public void callMethodTest() {
        ContractChannelCoupler coupler = new ContractChannelCoupler();
        coupler.setContract(contractMap, cache);

        contractMap.values().stream().forEach(c -> {
            TestContract s = (TestContract)c;
            s.channel = coupler;
        });
        TestContract t = (TestContract) contractMap.get("TEST2");
        t.callContractChannelInvoke("TEST1", "callMethod");

        t.callContractChannelQuery("TEST1","yesmanQuery");

    }

    @Test
    public void callChnnelMethodTest() {
        ContractChannelCoupler coupler = new ContractChannelCoupler();
        coupler.setContract(contractMap, cache);

        contractMap.values().stream().forEach(c -> {
            TestContract s = (TestContract)c;
            s.channel = coupler;
        });

        TestContract t = (TestContract) contractMap.get("TEST2");
        t.callContractChnnelMethod("TEST1", "transferChannel");

    }


}