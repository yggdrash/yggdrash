package io.yggdrash.node.api;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountApiImplTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);
    private static final AccountApi accountApi = new JsonRpcConfig().accountApi();

    @Test
    public void createAccountTest() {
        try {
            assertThat(accountApi.createAccount()).isNotEmpty();
        } catch (Exception exception) {
            log.debug("\n\ncreateAccountTest :: exception : " + exception);
        }
    }

    @Test
    public void accountsTest() {
        try {
            assertThat(accountApi.accounts()).isNotEmpty();
        } catch (Exception exception) {
            log.debug("accountsTest :: exception : " + exception);
        }
    }

    @Test
    public void getBalanceTest() {
        try {
            assertThat(accountApi.getBalance("0x2Aa4BCaC31F7F67B9a15681D5e4De2FBc778066A",
                    "latest")).isNotZero();
            assertThat(accountApi.getBalance("0x2Aa4BCaC31F7F67B9a15681D5e4De2FBc778066A",
                    "1023")).isNotZero();
        } catch (Exception exception) {
            log.debug("accountsTest :: exception : " + exception);
        }
    }
}
