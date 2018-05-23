package io.yggdrash.core.cache;

import io.yggdrash.core.Transaction;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.stereotype.Repository;

/**
 * The type Transaction repository.
 */
@Repository("yggdrash.transaction")
public class TransactionRepository {

    @Value("#{cacheManager.getCache('transactionPool')}")
    private ConcurrentMapCache transactionPool;

    /**
     * Gets transaction.
     *
     * @param hash the hash
     * @return the transaction
     */
    public Transaction getTransaction(byte[] hash) {
        // check Cache
        Transaction tx = transactionPool.get(hash, Transaction.class);

        // TODO getTransaction By Database
        return tx;


    }

    /**
     * Gets transaction.
     *
     * @param hashString the hash string
     * @return the transaction
     * @throws DecoderException the decoder exception
     */
    public Transaction getTransaction(String hashString) throws DecoderException {
        byte[] hash = Hex.decodeHex(hashString.toCharArray());
        // check Cache
        Transaction tx = transactionPool.get(hash, Transaction.class);

        // TODO getTransaction By Database
        return tx;


    }

    /**
     * Add transaction int.
     *
     * @param transaction the transaction
     * @return the int
     */
    public int addTransaction(Transaction transaction) {
        // FIXME get transaction hash value
        this.transactionPool.putIfAbsent(transaction.getHash(), transaction);

        // TODO insert Transaction to Databases


        return 0;
    }


}
