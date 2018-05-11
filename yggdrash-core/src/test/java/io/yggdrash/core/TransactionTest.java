package io.yggdrash.core;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TransactionTest {
    @Test
    public void transactionTest() throws IOException {
        Account account1 = new Account();
        Account account2 = new Account();
        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");
        Transaction t = new Transaction(account1, account2, json);



    }
}
