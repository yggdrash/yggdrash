package io.yggdrash.core.cache;


import com.google.gson.JsonObject;
import io.yggdrash.core.Account;
import io.yggdrash.core.Transaction;
import java.io.ByteArrayInputStream;
import org.apache.commons.codec.DecoderException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.concurrent.ConcurrentMapCache;
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
    public void addNewTransaction() throws IOException, DecoderException {
        Transaction tx = newTransaction();
        for (int i = 0; i < 100; i++) {
            Transaction tmpTx = newTransaction();
            log.debug(tmpTx.getHashString());
            txr.addTransaction(newTransaction(), false);
        }
        txr.addTransaction(tx, false);
        Transaction getTx = txr.getTransaction(tx.getHashString());
        assert getTx == tx;

    }

    @Test
    public void testTransactionPool() throws IOException {
        Transaction tx = newTransaction();
        log.debug("tx="+tx.toString());
        String hashString = tx.getHashString();

        ConcurrentMapCache transactionPool = new ConcurrentMapCache("test4");

        transactionPool.putIfAbsent(hashString,tx);

        for (int i = 0; i < 100; i++) {
            Transaction tmpTx = newTransaction();
            log.debug(tmpTx.getHashString());
            transactionPool.putIfAbsent(tmpTx.getHashString(), false);
        }

        Transaction tx2 = transactionPool.get(hashString, Transaction.class);
        log.debug("tx2=" + tx2.toString());
        assert tx == tx2;

    }


    /**
     * Test
     * @throws IOException
     */
    @Test
    public void flushAndLoad() throws IOException, DecoderException {
        Transaction tx = newTransaction();
        txr.addTransaction(tx, true);
        txr.flushPool();
        Transaction tx2 = txr.getTransaction(tx.getHashString());

        assert tx.getHashString().equals(tx2.getHashString());
    }

    @Test
    public void flushAndNotLoad() throws IOException, DecoderException {
        Transaction tx = newTransaction();
        txr.addTransaction(tx, false);
        txr.flushPool();
        Transaction tx2 = txr.getTransaction(tx.getHashString());

        assert tx2 == null;
    }


    public Transaction newTransaction() throws IOException {
        Account account = new Account();
        JsonObject json = new JsonObject();
        return new Transaction(account, json);
    }
}
