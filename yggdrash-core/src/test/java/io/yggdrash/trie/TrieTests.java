package io.yggdrash.trie;

import io.yggdrash.TestUtils;
import io.yggdrash.core.TransactionHusk;
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
        this.tx1 = TestUtils.createTransferTxHusk();
        this.tx2 = TestUtils.createTransferTxHusk();
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
        merkleRoot = Trie.getMerkleRootHusk(txsList);
        assertNotNull(merkleRoot);

        log.debug("MerkleRoot with tx 7=" + Hex.encodeHexString(merkleRoot));

        // 2. test with tx 1
        txsList = new ArrayList<>();
        txsList.add(this.tx1);
        merkleRoot = Trie.getMerkleRootHusk(txsList);
        assertNotNull(merkleRoot);

        log.debug("MerkleRoot with tx 1=" + Hex.encodeHexString(merkleRoot));

        // 3. test with tx 0
        txsList = new ArrayList<>();
        merkleRoot = Trie.getMerkleRootHusk(txsList);
        assertNull(merkleRoot);

        log.debug("MerkleRoot with tx 0 = null");

        // 4. test with tx null
        merkleRoot = Trie.getMerkleRootHusk(null);
        assertNull(merkleRoot);

        log.debug("MerkleRoot with tx null = null");

        // 5. null list Test
        txsList.add(this.tx1);
        txsList.add(this.tx2);
        merkleRoot = Trie.getMerkleRootHusk(txsList);
        assertNotNull(merkleRoot);

        txsList.add(null);
        merkleRoot = Trie.getMerkleRootHusk(txsList);
        assertNull(merkleRoot);

    }
}
