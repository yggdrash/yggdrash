package io.yggdrash.core.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

import io.yggdrash.core.Transaction;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
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
    DB db = null;

    public TransactionRepository(){
        // make database
        Options options = new Options();
        options.createIfMissing(true);
        try {
            // TODO resource path set by profile or setting file
            this.db = factory.open(new File("resources/db/transaction"), options);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets transaction.
     *
     * @param hash the hash
     * @return the transaction
     */
    public Transaction getTransaction(byte[] hash) {
        // check Cache
        Transaction tx = transactionPool.get(hash, Transaction.class);
        if (tx == null) {
            tx = loadTransactionIfExist(hash);
            if (tx != null) {
                transactionPool.putIfAbsent(hash, tx);
            }
        }
        return tx;
    }

    public void flushPool() {
        transactionPool.clear();
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
        return getTransaction(hash);
    }

    /**
     * Add transaction int.
     *
     * @param transaction the transaction
     */
    public void addTransaction(Transaction transaction, boolean store) {
        this.transactionPool.putIfAbsent(transaction.getHash(), transaction);
        if (store) {
            saveTransaction(transaction);
        }
    }

    /**
     * Save Transaction levelDB
     * @param transaction
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
                bos.close();
                out.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }

    /**
     * Load Transaction levelDb
     * @param hash transaction Hash
     * @return Transaction or null
     */
    private Transaction loadTransactionIfExist(byte[] hash){
        Transaction transaction = null;
        if (hash == null) {
            return null;
        }
        try {
            byte[] transactionBytes = this.db.get(hash);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(transactionBytes));
            transaction = (Transaction)ois.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // pass
        }
        return transaction;
    }


}
