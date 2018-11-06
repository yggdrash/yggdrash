package io.yggdrash.common.trie;

import io.yggdrash.TestUtils;
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
    public void MerkleRootSHA256Leaf1Test() {

        String algorithm = "SHA-256";

        // test1
        String[] inputs1 = {"0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree1 = new ArrayList<>();
        for (String input : inputs1) {
            tree1.add(Hex.decode(input));
        }

        byte[] result1 = Trie.getMerkleRoot(tree1, algorithm);
        log.info(Hex.toHexString(result1));
        assertEquals(Hex.toHexString(result1),
                "0123456789012345678901234567890123456789012345678901234567890123");

        ArrayList<byte[]> tree1Double = new ArrayList<>();
        for (String input : inputs1) {
            tree1Double.add(Hex.decode(input));
        }

        byte[] result1Double = Trie.getMerkleRoot(tree1Double, algorithm, true);
        log.info(Hex.toHexString(result1Double));
        assertEquals(Hex.toHexString(result1Double),
                "0123456789012345678901234567890123456789012345678901234567890123");
    }

    @Test
    public void MerkleRootSHA256Leaf2Test() {

        String algorithm = "SHA-256";

        // test2
        String[] inputs2 = {"0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree2 = new ArrayList<>();
        for (String input : inputs2) {
            tree2.add(Hex.decode(input));
        }

        byte[] result2 = Trie.getMerkleRoot(tree2, algorithm);
        log.info(Hex.toHexString(result2));
        assertEquals(Hex.toHexString(result2),
                "657eb7d33674849942a420f00f3f67e9195e2b003a646ec4dad2f8c9e4a9a5a9");

        ArrayList<byte[]> tree2Double = new ArrayList<>();
        for (String input : inputs2) {
            tree2Double.add(Hex.decode(input));
        }

        byte[] result2Double = Trie.getMerkleRoot(tree2Double, algorithm, true);
        log.info(Hex.toHexString(result2Double));
        assertEquals(Hex.toHexString(result2Double),
                "d6be2369c291d80a1663d990020705f36c1ab669d7d98248851304045dc20af2");

    }

    @Test
    public void MerkleRootSHA256Leaf3Test() {

        String algorithm = "SHA-256";

        // test3
        String[] inputs3 = {"0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree3 = new ArrayList<>();
        for (String input : inputs3) {
            tree3.add(Hex.decode(input));
        }

        byte[] result3 = Trie.getMerkleRoot(tree3, algorithm);
        log.info(Hex.toHexString(result3));
        assertEquals(Hex.toHexString(result3),
                "1fde90e914c974be374168a23dd4b764cf0ea6ff751af379932e810c33585540");

        ArrayList<byte[]> tree3Double = new ArrayList<>();
        for (String input : inputs3) {
            tree3Double.add(Hex.decode(input));
        }

        byte[] result3Double = Trie.getMerkleRoot(tree3Double, algorithm, true);
        log.info(Hex.toHexString(result3Double));
        assertEquals(Hex.toHexString(result3Double),
               "499360821ab840409ea59057c9452c1f3082ddea5ade6bea5ece3675c437fa5f");

    }

    @Test
    public void MerkleRootSHA256Leaf4Test() {

        String algorithm = "SHA-256";

        // test3
        String[] inputs = {"0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree = new ArrayList<>();
        for (String input : inputs) {
            tree.add(Hex.decode(input));
        }

        byte[] result = Trie.getMerkleRoot(tree, algorithm);
        log.info(Hex.toHexString(result));
        assertEquals(Hex.toHexString(result),
                "1fde90e914c974be374168a23dd4b764cf0ea6ff751af379932e810c33585540");

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        log.info(Hex.toHexString(resultDouble));
        assertEquals(Hex.toHexString(resultDouble),
                "499360821ab840409ea59057c9452c1f3082ddea5ade6bea5ece3675c437fa5f");

    }

    @Test
    public void MerkleRootSHA256Leaf5Test() {

        String algorithm = "SHA-256";

        // test3
        String[] inputs = {"0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree = new ArrayList<>();
        for (String input : inputs) {
            tree.add(Hex.decode(input));
        }

        byte[] result = Trie.getMerkleRoot(tree, algorithm);
        log.info(Hex.toHexString(result));
        assertEquals(Hex.toHexString(result),
                "fef5038a242cd86f80b49b873320d89399158c44bb4f20a1470e028c5f11e6a3");

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        log.info(Hex.toHexString(resultDouble));
        assertEquals(Hex.toHexString(resultDouble),
                "8cc04dba0093dd7da136c396a5f1fc27b2d2fbc104cf9eec412bc74da4ea3360");

    }

    @Test
    public void MerkleRootSHA3256Leaf1Test() {

        String algorithm = "SHA3-256";

        // test1
        String[] inputs1 = {"0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree1 = new ArrayList<>();
        for (String input : inputs1) {
            tree1.add(Hex.decode(input));
        }

        byte[] result1 = Trie.getMerkleRoot(tree1, algorithm);
        log.info(Hex.toHexString(result1));
        assertEquals(Hex.toHexString(result1),
                "0123456789012345678901234567890123456789012345678901234567890123");

        ArrayList<byte[]> tree1Double = new ArrayList<>();
        for (String input : inputs1) {
            tree1Double.add(Hex.decode(input));
        }

        byte[] result1Double = Trie.getMerkleRoot(tree1Double, algorithm, true);
        log.info(Hex.toHexString(result1Double));
        assertEquals(Hex.toHexString(result1Double),
                "0123456789012345678901234567890123456789012345678901234567890123");
    }

    @Test
    public void MerkleRootSHA3256Leaf2Test() {

        String algorithm = "SHA3-256";

        // test2
        String[] inputs2 = {"0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree2 = new ArrayList<>();
        for (String input : inputs2) {
            tree2.add(Hex.decode(input));
        }

        byte[] result2 = Trie.getMerkleRoot(tree2, algorithm);
        log.info(Hex.toHexString(result2));
        assertEquals(Hex.toHexString(result2),
                "a4a06b07a00cf0160b383b9686d01144b36bfdf30dd8d8916d27057e1e314b81");

        ArrayList<byte[]> tree2Double = new ArrayList<>();
        for (String input : inputs2) {
            tree2Double.add(Hex.decode(input));
        }

        byte[] result2Double = Trie.getMerkleRoot(tree2Double, algorithm, true);
        log.info(Hex.toHexString(result2Double));
        assertEquals(Hex.toHexString(result2Double),
                "5ca442bee9f7e49fea6f12a727f0a3d89149b762bbbb328f745cf57173b3dd36");

    }

    @Test
    public void MerkleRootSHA3256Leaf3Test() {

        String algorithm = "SHA3-256";

        // test3
        String[] inputs = {"0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree = new ArrayList<>();
        for (String input : inputs) {
            tree.add(Hex.decode(input));
        }

        byte[] result = Trie.getMerkleRoot(tree, algorithm);
        log.info(Hex.toHexString(result));
        assertEquals(Hex.toHexString(result),
                "cd3df5f3fc34e4c414011104827abbccc9c8a6c4db8b90bf63620ff1bdf51274");

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        log.info(Hex.toHexString(resultDouble));
        assertEquals(Hex.toHexString(resultDouble),
                "b128ac9fde01b3e89ef20e46f9a6bacb6b69515ecb795f98078fd9c8a6d1e14a");

    }

    @Test
    public void MerkleRootSHA3256Leaf4Test() {

        String algorithm = "SHA3-256";

        // test3
        String[] inputs = {"0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree = new ArrayList<>();
        for (String input : inputs) {
            tree.add(Hex.decode(input));
        }

        byte[] result = Trie.getMerkleRoot(tree, algorithm);
        log.info(Hex.toHexString(result));
        assertEquals(Hex.toHexString(result),
                "cd3df5f3fc34e4c414011104827abbccc9c8a6c4db8b90bf63620ff1bdf51274");

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        log.info(Hex.toHexString(resultDouble));
        assertEquals(Hex.toHexString(resultDouble),
                "b128ac9fde01b3e89ef20e46f9a6bacb6b69515ecb795f98078fd9c8a6d1e14a");

    }

    @Test
    public void MerkleRootSHA3256Leaf5Test() {

        String algorithm = "SHA3-256";

        // test3
        String[] inputs = {"0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree = new ArrayList<>();
        for (String input : inputs) {
            tree.add(Hex.decode(input));
        }

        byte[] result = Trie.getMerkleRoot(tree, algorithm);
        log.info(Hex.toHexString(result));
        assertEquals(Hex.toHexString(result),
                "2308c48b3d114b20c425b818ed9d740f3c311e47d5e0ed283a78a3d5fe338169");

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        log.info(Hex.toHexString(resultDouble));
        assertEquals(Hex.toHexString(resultDouble),
                "3a7a8543647800a660311d960e53ed9175b17ab58c12dacde4f41b3a2368bfa5");

    }

    @Test
    public void MerkleRootKECCAK256Leaf1Test() {

        String algorithm = "KECCAK-256";

        // test3
        String[] inputs = {"0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree = new ArrayList<>();
        for (String input : inputs) {
            tree.add(Hex.decode(input));
        }

        byte[] result = Trie.getMerkleRoot(tree, algorithm);
        log.info(Hex.toHexString(result));
        assertEquals(Hex.toHexString(result),
                "0123456789012345678901234567890123456789012345678901234567890123");

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        log.info(Hex.toHexString(resultDouble));
        assertEquals(Hex.toHexString(resultDouble),
                "0123456789012345678901234567890123456789012345678901234567890123");
    }

    @Test
    public void MerkleRootKECCAK256Leaf2Test() {

        String algorithm = "KECCAK-256";

        // test3
        String[] inputs = {"0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"
        };

        ArrayList<byte[]> tree = new ArrayList<>();
        for (String input : inputs) {
            tree.add(Hex.decode(input));
        }

        byte[] result = Trie.getMerkleRoot(tree, algorithm);
        log.info(Hex.toHexString(result));
        assertEquals(Hex.toHexString(result),
                "8a876df2135c3a2096b0232b3445ce959321c5874eef40d935c46669d4b69ca9");

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        log.info(Hex.toHexString(resultDouble));
        assertEquals(Hex.toHexString(resultDouble),
                "d788be966d089f0676c8cec86022086929b52884963915b0129e896c1ec5f3b2");
    }

    @Test
    public void MerkleRootKECCAK256Leaf3Test() {

        String algorithm = "KECCAK-256";

        // test3
        String[] inputs = {"0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"
        };

        ArrayList<byte[]> tree = new ArrayList<>();
        for (String input : inputs) {
            tree.add(Hex.decode(input));
        }

        byte[] result = Trie.getMerkleRoot(tree, algorithm);
        log.info(Hex.toHexString(result));
        assertEquals(Hex.toHexString(result),
                "bb5bd6e69fd04787dd31f73c76d0e219787a5548069dcef2c7cf8b701caa7fa9");

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        log.info(Hex.toHexString(resultDouble));
        assertEquals(Hex.toHexString(resultDouble),
                "0b718469b6e14b0872d1d81ebb893700c0a664c83c10162c9452afa536e71413");
    }

    @Test
    public void MerkleRootKECCAK256Leaf4Test() {

        String algorithm = "KECCAK-256";

        // test3
        String[] inputs = {"0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"
        };

        ArrayList<byte[]> tree = new ArrayList<>();
        for (String input : inputs) {
            tree.add(Hex.decode(input));
        }

        byte[] result = Trie.getMerkleRoot(tree, algorithm);
        log.info(Hex.toHexString(result));
        assertEquals(Hex.toHexString(result),
                "bb5bd6e69fd04787dd31f73c76d0e219787a5548069dcef2c7cf8b701caa7fa9");

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        log.info(Hex.toHexString(resultDouble));
        assertEquals(Hex.toHexString(resultDouble),
                "0b718469b6e14b0872d1d81ebb893700c0a664c83c10162c9452afa536e71413");
    }

    @Test
    public void MerkleRootKECCAK256Leaf5Test() {

        String algorithm = "KECCAK-256";

        // test3
        String[] inputs = {"0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"
        };

        ArrayList<byte[]> tree = new ArrayList<>();
        for (String input : inputs) {
            tree.add(Hex.decode(input));
        }

        byte[] result = Trie.getMerkleRoot(tree, algorithm);
        log.info(Hex.toHexString(result));
        assertEquals(Hex.toHexString(result),
                "fae6bbfc7c40c8ff7cdee46e2bb9c50bf544cae2f2c6851c5c1ef880c735dedd");

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        log.info(Hex.toHexString(resultDouble));
        assertEquals(Hex.toHexString(resultDouble),
                "88e84a3cb5986ffec233920ee531639ba63bb117e13b1195e57eda030bbc4879");
    }

}
