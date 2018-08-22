package io.yggdrash.node.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
            JsonArray params = new JsonArray();
            JsonObject param = new JsonObject();
            param.addProperty("address", "e1980adeafbb9ac6c9be60955484ab1547ab0b76");
            params.add(param);

            JsonObject query = new JsonObject();
            query.addProperty("address", "e1980adeafbb9ac6c9be60955484ab1547ab0b76");
            query.addProperty("method", "balanceOf");
            query.add("params", params);

            String qryString = query.toString();
            assertThat(accountApi.balanceOf(qryString)).isNotEmpty();
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
