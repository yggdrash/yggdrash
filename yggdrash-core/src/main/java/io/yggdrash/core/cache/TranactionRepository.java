package io.yggdrash.core.cache;

import io.yggdrash.core.Transaction;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.stereotype.Repository;

@Repository("yggdrach.transaction")
public class TranactionRepository {

    @Value("#{cacheManager.getCache('transactionPool')}")
    private ConcurrentMapCache transcationPool;

    // Transaction Cache
    public Transaction getTransaction(byte[] hash) {
        // check Cache
        Transaction tx = transcationPool.get(hash, Transaction.class);

        // TODO getTransaction By Database
        return tx;


    }
    public Transaction getTransaction(String hashString) throws DecoderException {
        byte[] hash = Hex.decodeHex(hashString.toCharArray());
        // check Cache
        Transaction tx = transcationPool.get(hash, Transaction.class);

        // TODO getTransaction By Database
        return tx;


    }

    public int addTranaction(Transaction transaction) {
        // FIXME get tranasction hash value
        transcationPool.putIfAbsent(transaction.getHash(), transaction);

        // TODO insert Transaction to Databases


        return 0;
    }



}
