package io.yggdrash;

import com.google.gson.JsonObject;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.WalletMock;
import io.yggdrash.trie.Trie;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertNotNull;

public class TrieTests {

    private Transaction tx1;
    private Transaction tx2;

    @Before
    public void setUp() {

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

        // create sample tx
        this.tx1 = new Transaction(data1);
        WalletMock.sign(tx1);

        this.tx2 = new Transaction(data2);
        WalletMock.sign(tx2);
    }

    @Test
    public void MerkleRootTest() throws IOException {

        // 1. test merkle root with tx 7
        // create transactions
        List<Transaction> txsList;
        txsList = new ArrayList<>();
        txsList.add(this.tx1);
        txsList.add(this.tx2);
        txsList.add(this.tx1);
        txsList.add(this.tx1);
        txsList.add(this.tx1);
        txsList.add(this.tx2);
        txsList.add(this.tx2);

        byte[] merkleRoot;
        merkleRoot = Trie.getMerkleRoot(txsList);
        assertNotNull(merkleRoot);

        System.out.println("MerkelRoot with tx 7=" + Hex.encodeHexString(merkleRoot));

        // 2. test with tx 1
        txsList = new ArrayList<>();
        txsList.add(this.tx1);
        merkleRoot = Trie.getMerkleRoot(txsList);
        assertNotNull(merkleRoot);

        System.out.println("MerkelRoot with tx 1=" + Hex.encodeHexString(merkleRoot));

        // 3. test with tx 0
        txsList = new ArrayList<>();
        merkleRoot = Trie.getMerkleRoot(txsList);
        assertNull(merkleRoot);

        System.out.println("MerkleRoot with tx 0 = null");

        // 4. test with tx null
        merkleRoot = Trie.getMerkleRoot(null);
        assertNull(merkleRoot);

        System.out.println("MerkleRoot with tx null = null");
    }
}
