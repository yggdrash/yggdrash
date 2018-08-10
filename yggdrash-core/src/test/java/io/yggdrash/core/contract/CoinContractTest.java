package io.yggdrash.core.contract;

import io.yggdrash.contract.CoinContract;
import io.yggdrash.contract.StateStore;
import io.yggdrash.core.TransactionReceipt;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CoinContractTest {

    private CoinContract coinContract;

    @Before
    public void setUp() throws Exception {
        StateStore stateStore = new StateStore();
        coinContract = new CoinContract(stateStore);
    }

    @Test
    public void balanceTest() {
        Integer res = coinContract.balance("aaa2aaab0fb041c5cb2a60a12291cbc3097352bb");
        assertThat(res).isEqualTo(10);
    }

    @Test
    public void transferTest() {
        TransactionReceipt res = coinContract.transfer(
                "aaa2aaab0fb041c5cb2a60a12291cbc3097352bb",
                "0x9843DC167956A0e5e01b3239a0CE2725c0631392",
                String.valueOf(10));
        assertThat(res.status).isEqualTo(1);
        assertThat(coinContract.balance("aaa2aaab0fb041c5cb2a60a12291cbc3097352bb"))
                .isEqualTo(0);
        assertThat(coinContract.balance("0x9843DC167956A0e5e01b3239a0CE2725c0631392"))
                .isEqualTo(10);
    }
}
