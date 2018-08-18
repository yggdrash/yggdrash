package io.yggdrash;

import io.yggdrash.core.husk.TransactionHusk;
import io.yggdrash.trie.Trie;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertNotNull;

public class TrieTests {

    private static final Logger log = LoggerFactory.getLogger(TrieTests.class);

    private TransactionHusk tx1;
    private TransactionHusk tx2;

    @Before
    public void setUp() {
        // create sample tx
        this.tx1 = TestUtils.createTxHusk();
        this.tx2 = TestUtils.createTxHusk();
    }

    @Test
    public void MerkleRootTest() {

        // 1. test merkle root with tx 7
        // create transactions
        List<TransactionHusk> txsList;
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

        log.debug("MerkelRoot with tx 7=" + Hex.encodeHexString(merkleRoot));

        // 2. test with tx 1
        txsList = new ArrayList<>();
        txsList.add(this.tx1);
        merkleRoot = Trie.getMerkleRoot(txsList);
        assertNotNull(merkleRoot);

        log.debug("MerkelRoot with tx 1=" + Hex.encodeHexString(merkleRoot));

        // 3. test with tx 0
        txsList = new ArrayList<>();
        merkleRoot = Trie.getMerkleRoot(txsList);
        assertNull(merkleRoot);

        log.debug("MerkleRoot with tx 0 = null");

        // 4. test with tx null
        merkleRoot = Trie.getMerkleRoot(null);
        assertNull(merkleRoot);

        log.debug("MerkleRoot with tx null = null");
    }
}
