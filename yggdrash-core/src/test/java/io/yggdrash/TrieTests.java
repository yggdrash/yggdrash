package io.yggdrash;

import com.google.gson.JsonObject;
import io.yggdrash.core.Account;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.exception.NotValidteException;
import io.yggdrash.trie.Trie;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertNotNull;

public class TrieTests {
    private static final Logger log = LoggerFactory.getLogger(Trie.class);

    public Account from;
    public Transaction tx1;
    public Transaction tx2;

    @Before
    public void setUp() throws Exception {

        // create tx_data1
        JsonObject data1 = new JsonObject();
        data1.addProperty("key", "balance");
        data1.addProperty("operator", "transfer");
        data1.addProperty("value", 30);

        // create tx_data2
        JsonObject data2 = new JsonObject();
        data2.addProperty("key", "balance");
        data2.addProperty("operator", "transfer");
        data2.addProperty("value", 10);

        // create account
        this.from = new Account();

        // create sample tx
        this.tx1 = new Transaction(from, data1);
        this.tx2 = new Transaction(from, data2);

    }

    @Test
    public void MerkleRootTest() throws IOException, NotValidteException {

        byte[] merkle_root;

        // 1. test merkle root with tx 7
        // create transactions
        List<Transaction> txs_list;
        txs_list = new ArrayList<Transaction>();
        txs_list.add(this.tx1);
        txs_list.add(this.tx2);
        txs_list.add(this.tx1);
        txs_list.add(this.tx1);
        txs_list.add(this.tx1);
        txs_list.add(this.tx2);
        txs_list.add(this.tx2);

        merkle_root = Trie.getMerkleRoot(txs_list);
        assertNotNull(merkle_root);

        if (merkle_root != null) {
            System.out.println("MerkelRoot with tx 7=" + Hex.encodeHexString(merkle_root));
        } else {
            System.out.println("MerkleRoot with tx 7 = null");
        }


        // 2. test with tx 1
        txs_list = new ArrayList<Transaction>();
        txs_list.add(this.tx1);
        merkle_root = Trie.getMerkleRoot(txs_list);
        assertNotNull(merkle_root);

        if (merkle_root != null) {
            System.out.println("MerkelRoot with tx 1=" + Hex.encodeHexString(merkle_root));
        } else {
            System.out.println("MerkleRoot with tx 1 = null");
        }


        // 3. test with tx 0
        txs_list = new ArrayList<Transaction>();
        merkle_root = Trie.getMerkleRoot(txs_list);
        assertNull(merkle_root);

        if (merkle_root != null) {
            System.out.println("MerkelRoot with tx 0=" + Hex.encodeHexString(merkle_root));
        } else {
            System.out.println("MerkleRoot with tx 0 = null");
        }


        // 4. test with tx null
        merkle_root = Trie.getMerkleRoot(null);
        assertNull(merkle_root);

        if (merkle_root != null) {
            System.out.println("MerkelRoot with tx null=" + Hex.encodeHexString(merkle_root));
        } else {
            System.out.println("MerkleRoot with tx null = null");
        }

    }
}
