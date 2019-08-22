package io.yggdrash.contract.token;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TokenContractTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void start() {
    }

    @Test
    public void stop() {
    }

    @Test
    public void serviceChanged() {
    }

    private void ref() {
        TokenContract contract = new TokenContract();
        TokenContract.TokenService service = new TokenContract.TokenService();

        System.out.println(service.approve(null));
        System.out.println(service.allowance(null));
        System.out.println(service.totalSupply(null));
        System.out.println(service.balanceOf(null));
        System.out.println(service.burn(null));
        System.out.println(service.closeExchange(null));
        System.out.println(service.createToken(null));
        System.out.println(service.depositYeedStakeToToken(null));
        System.out.println(service.destroyToken(null));
        System.out.println(service.exchangeT2T(null));
        System.out.println(service.exchangeT2Y(null));
        System.out.println(service.exchangeY2T(null));
        System.out.println(service.getTokenBalanceOf(null));
        System.out.println(service.getYeedBalanceOf(null));
        System.out.println(service.init(null));
        System.out.println(service.mint(null));
        System.out.println(service.movePhasePause(null));
        System.out.println(service.movePhaseRun(null));
        System.out.println(service.movePhaseStop(null));
        System.out.println(service.openExchange(null));
        System.out.println(service.transfer(null));
        System.out.println(service.transferFrom(null));
        System.out.println(service.withdrawYeedStakeFromToken(null));
    }
}