package io.yggdrash.node.api;

import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.yggdrash.node.api.JsonRpcConfig.ACCOUNT_API;
import static org.assertj.core.api.Assertions.assertThat;

public class AccountApiImplTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);

    @Test
    public void createAccountTest() {
        try {
            assertThat(ACCOUNT_API.createAccount()).isNotEmpty();
        } catch (Exception exception) {
            log.debug("\n\ncreateAccountTest :: exception : " + exception);
        }
    }

    @Test
    public void accountsTest() {
        try {
            assertThat(ACCOUNT_API.accounts()).isNotEmpty();
        } catch (Exception exception) {
            log.debug("accountsTest :: exception : " + exception);
        }
    }

    @Test
    public void balanceOfTest() {
        try {
            JsonObject param = ContractTestUtils.createParam("address",
                    "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
            JsonObject query = ContractTestUtils.createQuery(TestConstants.YEED,
                    "balanceOf", param);

            assertThat(ACCOUNT_API.balanceOf(query.toString())).isNotEmpty();
        } catch (Exception e) {
            log.debug("\nbalanceOfTest :: exception : " + e);
        }
    }

    @Test
    public void getBalanceTest() {
        try {
            assertThat(ACCOUNT_API.getBalance("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94",
                    "latest")).isNotZero();
            assertThat(ACCOUNT_API.getBalance("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94",
                    "1023")).isNotZero();
        } catch (Exception e) {
            log.debug("getBalanceTest :: exception : " + e);
        }
    }
}
