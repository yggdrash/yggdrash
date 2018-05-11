package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.core.cache.CacheConfigurationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.HashMap;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TransactionTest {
    private static final Logger log = LoggerFactory.getLogger(TransactionTest.class);

    @Test
    public void transactionTest() throws IOException {
        Account account1 = new Account();
        Account account2 = new Account();
        JsonObject json = new JsonObject();
        json.addProperty("data", "TEST");
        Transaction t = new Transaction(account1, account2, json);
    }

    @Test
    public void makeTransactionTest() throws IOException {
        // check transaction
        int makeTransaction = 10000;

        HashMap<Integer,Transaction> hash = new HashMap<>();
        for(int i=0;i<makeTransaction;i++) {
            Transaction tx = addNewTransaction();
            log.debug("hashcode : " + tx.hashCode());
            assert hash.get(tx.hashCode()) == null;
            hash.put(tx.hashCode(), tx);
        }

    }

    public Transaction addNewTransaction() throws IOException {
        Account account = new Account();
        JsonObject json = new JsonObject();
        return new Transaction(account, account, json);
    }


}
