package io.yggdrash.core.cache;


import com.google.gson.JsonObject;
import io.yggdrash.core.Account;
import io.yggdrash.core.Transaction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CacheConfigurationTest {

    @Autowired
    ApplicationContext applicationContext;

    private static final Logger log = LoggerFactory.getLogger(CacheConfigurationTest.class);

    @Value("#{cacheManager.getCache('unconfirmTransaction')}")
    private ConcurrentMapCache uTx;

    @Autowired
    private Object cacheManager;

    @Test
    public void uTxCache() throws IOException {
        assert uTx.getName() != "";
        Account account = new Account();
        JsonObject json = new JsonObject();
        uTx.put("TEST0", new Transaction(account, account, json));
        uTx.put("TEST1", new Transaction(account, account, json));
        uTx.put("TEST2", new Transaction(account, account, json));

        Transaction tx = uTx.get("TEST0", Transaction.class);

//        log.debug("" + tx.getTimestamp());
//        assert tx.getTimestamp() != 0L;

        ConcurrentMap eh = uTx.getNativeCache();
        Iterator<Object> list = eh.keySet().iterator();
        Object key;
        while (list.hasNext()) {
            key = list.next();
            log.debug(key + "" + uTx.get(key).toString());
        }
        assert eh.size() > 0;

        uTx.clear();
    }

    @Test
    public void uTxCacheClear() {
        Cache confirm = ((CacheManager) this.cacheManager).getCache("confirmBlock");
        log.debug(confirm.getName());

        log.debug(cacheManager.getClass().getName());
        log.debug(cacheManager.toString());


    }

    @Test
    public void flushCache() throws IOException {
        int testCount = 100000;
        for (int i = 0; i < testCount; i++) {
            addNewTransaction();
        }
        log.debug("Size : " + uTx.getNativeCache().size());
        assert uTx.getNativeCache().size() == testCount;

        Iterator<Object> unconfirmTransaction = uTx.getNativeCache().keySet().iterator();
        List<Object> unconfirmList = new ArrayList<>();
        unconfirmTransaction.forEachRemaining(unconfirmList::add);
        unconfirmList.forEach(uTx::evict);

        assert uTx.getNativeCache().size() == 0;
    }

    public void addNewTransaction() throws IOException {
        Account account = new Account();
        JsonObject json = new JsonObject();
        Transaction tx = new Transaction(account, account, json);
        // Transaction has random hashcode
        if (uTx.get(tx.getHash()) != null) {
            addNewTransaction();
        } else {
            uTx.put(tx.getHash(), tx);
        }

    }

}
