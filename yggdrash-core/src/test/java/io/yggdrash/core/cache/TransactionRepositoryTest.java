package io.yggdrash.core.cache;


import com.google.gson.JsonObject;
import io.yggdrash.core.Account;
import io.yggdrash.core.Transaction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TransactionRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionRepositoryTest.class);
    @Resource(name = "yggdrash.transaction")
    private TransactionRepository txr;

    @Test
    public void addNewTransaction() throws IOException {
        Transaction tx = newTransaction();
        for (int i = 0; i < 100; i++) {
            Transaction tmpTx = newTransaction();
            log.debug(tmpTx.getHashString());
            txr.addTransaction(newTransaction());
        }
        txr.addTransaction(tx);
        Transaction getTx = txr.getTransaction(tx.getHash());
        assert getTx == tx;

    }

    public Transaction newTransaction() throws IOException {
        Account account = new Account();
        JsonObject json = new JsonObject();
        return new Transaction(account, json);
    }
}
