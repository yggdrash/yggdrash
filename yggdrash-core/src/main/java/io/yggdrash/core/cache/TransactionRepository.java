package io.yggdrash.core.cache;

import io.yggdrash.core.Transaction;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.stereotype.Repository;

@Repository("yggdrash.transaction")
public class TransactionRepository {

    @Value("#{cacheManager.getCache('transactionPool')}")
    private ConcurrentMapCache transactionPool;

    /**
     * get Transaction Cache
     * @param hash
     * @return
     */
    public Transaction getTransaction(byte[] hash) {
        // check Cache
        Transaction tx = transactionPool.get(hash, Transaction.class);

        // TODO getTransaction By Database
        return tx;


    }

    /**
     * get Transaction By hashString from Transaction Cache
     * @param hashString
     * @return
     * @throws DecoderException
     */
    public Transaction getTransaction(String hashString) throws DecoderException {
        byte[] hash = Hex.decodeHex(hashString.toCharArray());
        // check Cache
        Transaction tx = transactionPool.get(hash, Transaction.class);

        // TODO getTransaction By Database
        return tx;


    }

    /**
     * add Transaction to Transaction Cache
     * @param transaction
     * @return
     */
    public int addTransaction(Transaction transaction) {
        // FIXME get transaction hash value
        this.transactionPool.putIfAbsent(transaction.getHash(), transaction);

        // TODO insert Transaction to Databases


        return 0;
    }



}
