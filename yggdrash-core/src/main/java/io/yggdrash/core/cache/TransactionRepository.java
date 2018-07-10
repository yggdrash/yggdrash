package io.yggdrash.core.cache;

import io.yggdrash.core.Transaction;
import io.yggdrash.core.datasource.LevelDbDataSource;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Objects;

/**
 * The type Transaction repository.
 */
@Repository("yggdrash.transaction")
public class TransactionRepository {
    private final LevelDbDataSource db;

    private static final Logger log = LoggerFactory.getLogger(TransactionRepository.class);

    @Value("#{cacheManager.getCache('transactionPool')}")
    private ConcurrentMapCache transactionPool;

    @Autowired
    public TransactionRepository(LevelDbDataSource db) {
        this.db = db;
        this.db.init();
    }

    /**
     * Gets transaction.
     *
     * @param hashString the hash
     * @return the transaction
     */
    public Transaction getTransaction(String hashString) throws DecoderException {

        // check Cache
        Transaction tx = transactionPool.get(hashString, Transaction.class);
        log.debug("get transaction hash : " + hashString);

        if (tx == null) {
            tx = loadTransactionIfExist(hashString);
            if (tx != null) {
                transactionPool.putIfAbsent(hashString, tx);
            }
        }
        return tx;
    }

    public void flushPool() {
        transactionPool.clear();
    }

    /**
     * Add transaction int.
     *
     * @param transaction the transaction
     */
    public void addTransaction(Transaction transaction, boolean store) throws IOException {
        this.transactionPool.putIfAbsent(transaction.getHashString(), transaction);
        //todo: check from byte[] to object
        log.debug("add transaction hash : " + transaction.getHashString());

        if (store) {
            saveTransaction(transaction);
        }
    }

    /**
     * Save Transaction levelDB
     *
     * @param transaction the transaction
     */
    private void saveTransaction(Transaction transaction) {
        ObjectOutput out = null;
        ByteArrayOutputStream bos = null;
        try {
            bos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bos);
            out.writeObject(transaction);
            out.flush();
            byte[] transactionBytes = bos.toByteArray();
            this.db.put(transaction.getHash(), transactionBytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                Objects.requireNonNull(bos).close();
                Objects.requireNonNull(out).close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }

    /**
     * Load Transaction levelDb
     *
     * @param hashString transaction Hash
     * @return Transaction or null
     */
    private Transaction loadTransactionIfExist(String hashString) throws DecoderException {
        Transaction transaction = null;
        if (hashString == null) {
            return null;
        }
        try {
            byte[] transactionBytes = this.db.get(Hex.decodeHex(hashString.toCharArray()));
            ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream(transactionBytes));
            transaction = (Transaction) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // pass
        }
        return transaction;
    }


}
