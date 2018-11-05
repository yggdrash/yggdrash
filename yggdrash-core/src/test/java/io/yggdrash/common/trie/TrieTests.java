package io.yggdrash.common.trie;

import io.yggdrash.TestUtils;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.core.TransactionHusk;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
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

        log.debug("MerkleRoot with tx 7=" + Hex.toHexString(merkleRoot));

        // 2. test with tx 1
        txsList = new ArrayList<>();
        txsList.add(this.tx1);
        merkleRoot = Trie.getMerkleRootHusk(txsList);
        assertNotNull(merkleRoot);

        log.debug("MerkleRoot with tx 1=" + Hex.toHexString(merkleRoot));

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

    @Test
    public void MerkleRootStaticTest() {

        String[] inputs = {"0123456789012345678901234567890123456789012345678901234567890123",
//                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree = new ArrayList<>();
        for (String input : inputs) {
            tree.add(org.spongycastle.util.encoders.Hex.decode(input));
        }

        Trie.calculateMerkle(tree, tree.size());


        log.info(Hex.toHexString(tree.get(tree.size() - 1)));
        String result2 = Hex.toHexString(HashUtil.sha256(tree.get(tree.size() - 1)));
        log.info(result2);

        //assertEquals(result1, result2);
    }


    @Test
    public void MerkleRootStaticTest2() {

        String[] inputs = {"0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree = new ArrayList<>();
        for (String input : inputs) {
            tree.add(Hex.decode(input));
        }

        Trie.calculateMerkle(tree, tree.size());

        for (byte[] t : tree) {
            log.info(Hex.toHexString(t));
        }

        log.info(Hex.toHexString(tree.get(tree.size() - 1)));

        String result2 = Hex.toHexString(HashUtil.sha256(tree.get(tree.size() - 1)));
        log.info(result2);

        //assertEquals(result1, result2);

        String input3 = "0123456789012345678901234567890123456789012345678901234567890123"
                + "0123456789012345678901234567890123456789012345678901234567890123";

        log.info(Hex.toHexString(HashUtil.sha256(Hex.decode(input3))));
        log.info(Hex.toHexString(HashUtil.sha256(HashUtil.sha256(Hex.decode(input3)))));

    }


    @Test
    public void MerkleRootStaticTest3() {

        String[] inputs = {"0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree = new ArrayList<>();
        for (String input : inputs) {
            tree.add(Hex.decode(input));
        }

        Trie.calculateMerkle(tree, tree.size());

        for (byte[] t : tree) {
            log.info(Hex.toHexString(t));
        }

        log.info(Hex.toHexString(tree.get(tree.size() - 1)));

        String result2 = Hex.toHexString(HashUtil.sha256(tree.get(tree.size() - 1)));
        log.info(result2);

        String result3 = Hex.toHexString(HashUtil.sha256(HashUtil.sha256(tree.get(tree.size() - 1))));
        log.info(result3);

    }

    @Test
    public void MerkleRootStaticTest3-1() {

        String[] inputs = {"0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree = new ArrayList<>();
        for (String input : inputs) {
            tree.add(Hex.decode(input));
        }

        Trie.calculateMerkle(tree, tree.size());

        for (byte[] t : tree) {
            log.info(Hex.toHexString(t));
        }

        log.info(Hex.toHexString(tree.get(tree.size() - 1)));

        String result2 = Hex.toHexString(HashUtil.sha256(tree.get(tree.size() - 1)));
        log.info(result2);

        String result3 = Hex.toHexString(HashUtil.sha256(HashUtil.sha256(tree.get(tree.size() - 1))));
        log.info(result3);

    }

    @Test
    public void HashTest() {

        String input = "0";

        String sha1 = Hex.toHexString(HashUtil.sha1(input.getBytes()));
        String sha2 = Hex.toHexString(HashUtil.sha256(input.getBytes()));
        String sha3 = Hex.toHexString(HashUtil.sha3(input.getBytes()));

        log.info("sha1=" + sha1);
        assertEquals(sha1, "b6589fc6ab0dc82cf12099d1c2d40ab994e8410c");

        log.info("sha2=" + sha2);
        assertEquals(sha2, "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9");

        log.info("sha3=" + sha3);
        assertEquals(sha3, "044852b2a670ade5407e78fb2863c51de9fcb96542a07186fe3aeda6bb8a116d");

    }


}
