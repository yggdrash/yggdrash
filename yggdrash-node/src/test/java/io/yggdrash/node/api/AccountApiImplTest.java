package io.yggdrash.node.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.contract.ContractQry;
import org.apache.commons.codec.binary.Hex;
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
    public void balanceOfTest() {
        try {
            JsonObject qry = ContractQry.createQuery(
                    Hex.encodeHexString(TestUtils.YEED_CHAIN),
                    "balanceOf",
                    ContractQry.createParams("address", "e1980adeafbb9ac6c9be60955484ab1547ab0b76"));

            assertThat(accountApi.balanceOf(qry.toString())).isNotEmpty();
        } catch (Exception e) {
            log.debug("\nbalanceOfTest :: exception : " + e);
        }
    }

    @Test
    public void getBalanceTest() {
        try {
            assertThat(accountApi.getBalance("e1980adeafbb9ac6c9be60955484ab1547ab0b76",
                    "latest")).isNotZero();
            assertThat(accountApi.getBalance("e1980adeafbb9ac6c9be60955484ab1547ab0b76",
                    "1023")).isNotZero();
        } catch (Exception e) {
            log.debug("getBalanceTest :: exception : " + e);
        }
    }
}
