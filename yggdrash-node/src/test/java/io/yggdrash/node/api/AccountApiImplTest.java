package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountApiImplTest {
    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);

    private final JsonRpcHttpClient jsonRpcHttpClient = new JsonRpcConfig().jsonRpcHttpClient();

    @Test
    public void createAccountTest() {
        try {
            AccountApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    AccountApi.class, jsonRpcHttpClient);
            assertThat(api).isNotNull();
            assertThat(api.createAccount()).isNotEmpty();
        } catch (Exception exception) {
            log.debug("\n\ncreateAccountTest :: exception : " + exception);
        }
    }

    @Test
    public void accountsTest() {
        try {
            AccountApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    AccountApi.class, jsonRpcHttpClient);
            assertThat(api).isNotNull();
            assertThat(api.accounts()).isNotEmpty();
        } catch (Exception exception) {
            log.debug("accountsTest :: exception : " + exception);
        }
    }

    @Test
    public void getBalanceTest() {
        try {
            AccountApi api = ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    AccountApi.class, jsonRpcHttpClient);
            assertThat(api).isNotNull();

            assertThat(api.getBalance("0x2Aa4BCaC31F7F67B9a15681D5e4De2FBc778066A",
                    "latest")).isNotZero();
            assertThat(api.getBalance("0x2Aa4BCaC31F7F67B9a15681D5e4De2FBc778066A",
                    "1023")).isNotZero();
        } catch (Exception exception) {
            log.debug("accountsTest :: exception : " + exception);
        }
    }
}
