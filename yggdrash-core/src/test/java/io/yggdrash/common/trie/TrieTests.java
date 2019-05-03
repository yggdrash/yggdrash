package io.yggdrash.common.trie;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.common.config.Constants;
import io.yggdrash.core.blockchain.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TrieTests {

    private static final Logger log = LoggerFactory.getLogger(TrieTests.class);

    private Transaction tx1;
    private Transaction tx2;

    @Before
    public void setUp() {
        // create sample tx
        this.tx1 = BlockChainTestUtils.createTransferTx();
        this.tx2 = BlockChainTestUtils.createTransferTx();
    }

    @Test
    public void MerkleRootTest() {

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
        assertThat(merkleRoot).isNotEqualTo(Constants.EMPTY_HASH);

        log.debug("MerkleRoot with tx 7=" + Hex.toHexString(merkleRoot));

        // 2. test with tx 1
        txsList = new ArrayList<>();
        txsList.add(this.tx1);
        merkleRoot = Trie.getMerkleRoot(txsList);
        assertThat(merkleRoot).isNotEqualTo(Constants.EMPTY_HASH);

        log.debug("MerkleRoot with tx 1=" + Hex.toHexString(merkleRoot));

        // 3. test with tx 0
        txsList = new ArrayList<>();
        merkleRoot = Trie.getMerkleRoot(txsList);
        assertArrayEquals(Constants.EMPTY_HASH, merkleRoot);

        log.debug("MerkleRoot with tx 0 = null");

        // 4. test with tx null
        merkleRoot = Trie.getMerkleRoot(null);
        assertArrayEquals(Constants.EMPTY_HASH, merkleRoot);

        log.debug("MerkleRoot with tx null = null");

        // 5. null list Test
        txsList.add(null);
        merkleRoot = Trie.getMerkleRoot(txsList);
        assertArrayEquals(Constants.EMPTY_HASH, merkleRoot);
    }

    @Test
    public void MerkleRootSha256Leaf1Test() {

        String algorithm = "SHA-256";

        // test1
        String[] inputs1 = {"0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree1 = new ArrayList<>();
        for (String input : inputs1) {
            tree1.add(Hex.decode(input));
        }

        byte[] result1 = Trie.getMerkleRoot(tree1, algorithm);
        log.info(Hex.toHexString(result1));
        assertEquals("0123456789012345678901234567890123456789012345678901234567890123",
                Hex.toHexString(result1));

        ArrayList<byte[]> tree1Double = new ArrayList<>();
        for (String input : inputs1) {
            tree1Double.add(Hex.decode(input));
        }

        byte[] result1Double = Trie.getMerkleRoot(tree1Double, algorithm, true);
        assert result1Double != null;
        log.info(Hex.toHexString(result1Double));
        assertEquals("0123456789012345678901234567890123456789012345678901234567890123",
                Hex.toHexString(result1Double));
    }

    @Test
    public void MerkleRootSha256Leaf2Test() {

        String algorithm = "SHA-256";

        String[] inputs = {"0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree = new ArrayList<>();
        for (String input : inputs) {
            tree.add(Hex.decode(input));
        }

        byte[] result = Trie.getMerkleRoot(tree, algorithm);
        log.info(Hex.toHexString(result));
        assertEquals("657eb7d33674849942a420f00f3f67e9195e2b003a646ec4dad2f8c9e4a9a5a9",
                Hex.toHexString(result));

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        assert resultDouble != null;
        log.info(Hex.toHexString(resultDouble));
        assertEquals("d6be2369c291d80a1663d990020705f36c1ab669d7d98248851304045dc20af2",
                Hex.toHexString(resultDouble));

    }

    @Test
    public void MerkleRootSha256Leaf3Test() {

        String algorithm = "SHA-256";

        String[] inputs = {"0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123",
                "0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree = new ArrayList<>();
        for (String input : inputs) {
            tree.add(Hex.decode(input));
        }

        byte[] result = Trie.getMerkleRoot(tree, algorithm);
        log.info(Hex.toHexString(result));
        assertEquals("1fde90e914c974be374168a23dd4b764cf0ea6ff751af379932e810c33585540", Hex.toHexString(result));

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        assert resultDouble != null;
        log.info(Hex.toHexString(resultDouble));
        assertEquals("499360821ab840409ea59057c9452c1f3082ddea5ade6bea5ece3675c437fa5f", Hex.toHexString(resultDouble));

    }

    @Test
    public void MerkleRootSha256Leaf4Test() {

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
        assertEquals("1fde90e914c974be374168a23dd4b764cf0ea6ff751af379932e810c33585540", Hex.toHexString(result));

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        assert resultDouble != null;
        log.info(Hex.toHexString(resultDouble));
        assertEquals("499360821ab840409ea59057c9452c1f3082ddea5ade6bea5ece3675c437fa5f", Hex.toHexString(resultDouble));

    }

    @Test
    public void MerkleRootSha256Leaf5Test() {

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
        assertEquals("fef5038a242cd86f80b49b873320d89399158c44bb4f20a1470e028c5f11e6a3", Hex.toHexString(result));

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        assert resultDouble != null;
        log.info(Hex.toHexString(resultDouble));
        assertEquals("8cc04dba0093dd7da136c396a5f1fc27b2d2fbc104cf9eec412bc74da4ea3360", Hex.toHexString(resultDouble));

    }

    @Test
    public void MerkleRootSha3256Leaf1Test() {

        String algorithm = "SHA3-256";

        // test1
        String[] inputs1 = {"0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree1 = new ArrayList<>();
        for (String input : inputs1) {
            tree1.add(Hex.decode(input));
        }

        byte[] result1 = Trie.getMerkleRoot(tree1, algorithm);
        log.info(Hex.toHexString(result1));
        assertEquals("0123456789012345678901234567890123456789012345678901234567890123",
                Hex.toHexString(result1));

        ArrayList<byte[]> tree1Double = new ArrayList<>();
        for (String input : inputs1) {
            tree1Double.add(Hex.decode(input));
        }

        byte[] result1Double = Trie.getMerkleRoot(tree1Double, algorithm, true);
        assert result1Double != null;
        log.info(Hex.toHexString(result1Double));
        assertEquals("0123456789012345678901234567890123456789012345678901234567890123",
                Hex.toHexString(result1Double));
    }

    @Test
    public void MerkleRootSha3256Leaf2Test() {

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
        assertEquals("a4a06b07a00cf0160b383b9686d01144b36bfdf30dd8d8916d27057e1e314b81",
                Hex.toHexString(result2));

        ArrayList<byte[]> tree2Double = new ArrayList<>();
        for (String input : inputs2) {
            tree2Double.add(Hex.decode(input));
        }

        byte[] result2Double = Trie.getMerkleRoot(tree2Double, algorithm, true);
        assert result2Double != null;
        log.info(Hex.toHexString(result2Double));
        assertEquals("5ca442bee9f7e49fea6f12a727f0a3d89149b762bbbb328f745cf57173b3dd36",
                Hex.toHexString(result2Double));

    }

    @Test
    public void MerkleRootSha3256Leaf3Test() {

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
        assertEquals("cd3df5f3fc34e4c414011104827abbccc9c8a6c4db8b90bf63620ff1bdf51274",
                Hex.toHexString(result));

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        assert resultDouble != null;
        log.info(Hex.toHexString(resultDouble));
        assertEquals("b128ac9fde01b3e89ef20e46f9a6bacb6b69515ecb795f98078fd9c8a6d1e14a",
                Hex.toHexString(resultDouble));

    }

    @Test
    public void MerkleRootSha3256Leaf4Test() {

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
        assertEquals("cd3df5f3fc34e4c414011104827abbccc9c8a6c4db8b90bf63620ff1bdf51274",
                Hex.toHexString(result));

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        assert resultDouble != null;
        log.info(Hex.toHexString(resultDouble));
        assertEquals("b128ac9fde01b3e89ef20e46f9a6bacb6b69515ecb795f98078fd9c8a6d1e14a",
                Hex.toHexString(resultDouble));

    }

    @Test
    public void MerkleRootSha3256Leaf5Test() {

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
        assertEquals("2308c48b3d114b20c425b818ed9d740f3c311e47d5e0ed283a78a3d5fe338169",
                Hex.toHexString(result));

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        assert resultDouble != null;
        log.info(Hex.toHexString(resultDouble));
        assertEquals("3a7a8543647800a660311d960e53ed9175b17ab58c12dacde4f41b3a2368bfa5",
                Hex.toHexString(resultDouble));

    }

    @Test
    public void MerkleRootKeccak256Leaf1Test() {

        String algorithm = "KECCAK-256";

        // test3
        String[] inputs = {"0123456789012345678901234567890123456789012345678901234567890123"};

        ArrayList<byte[]> tree = new ArrayList<>();
        for (String input : inputs) {
            tree.add(Hex.decode(input));
        }

        byte[] result = Trie.getMerkleRoot(tree, algorithm);
        log.info(Hex.toHexString(result));
        assertEquals("0123456789012345678901234567890123456789012345678901234567890123",
                Hex.toHexString(result));

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        assert resultDouble != null;
        log.info(Hex.toHexString(resultDouble));
        assertEquals("0123456789012345678901234567890123456789012345678901234567890123",
                Hex.toHexString(resultDouble));
    }

    @Test
    public void MerkleRootKeccak256Leaf2Test() {

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
        assertEquals("8a876df2135c3a2096b0232b3445ce959321c5874eef40d935c46669d4b69ca9",
                Hex.toHexString(result));

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        assert resultDouble != null;
        log.info(Hex.toHexString(resultDouble));
        assertEquals("d788be966d089f0676c8cec86022086929b52884963915b0129e896c1ec5f3b2",
                Hex.toHexString(resultDouble));
    }

    @Test
    public void MerkleRootKeccak256Leaf3Test() {

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
        assertEquals("bb5bd6e69fd04787dd31f73c76d0e219787a5548069dcef2c7cf8b701caa7fa9",
                Hex.toHexString(result));

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        assert resultDouble != null;
        log.info(Hex.toHexString(resultDouble));
        assertEquals("0b718469b6e14b0872d1d81ebb893700c0a664c83c10162c9452afa536e71413",
                Hex.toHexString(resultDouble));
    }

    @Test
    public void MerkleRootKeccak256Leaf4Test() {

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
        assertEquals("bb5bd6e69fd04787dd31f73c76d0e219787a5548069dcef2c7cf8b701caa7fa9",
                Hex.toHexString(result));

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        assert resultDouble != null;
        log.info(Hex.toHexString(resultDouble));
        assertEquals("0b718469b6e14b0872d1d81ebb893700c0a664c83c10162c9452afa536e71413",
                Hex.toHexString(resultDouble));
    }

    @Test
    public void MerkleRootKeccak256Leaf5Test() {

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
        assertEquals("fae6bbfc7c40c8ff7cdee46e2bb9c50bf544cae2f2c6851c5c1ef880c735dedd",
                Hex.toHexString(result));

        ArrayList<byte[]> treeDouble = new ArrayList<>();
        for (String input : inputs) {
            treeDouble.add(Hex.decode(input));
        }

        byte[] resultDouble = Trie.getMerkleRoot(treeDouble, algorithm, true);
        assert resultDouble != null;
        log.info(Hex.toHexString(resultDouble));
        assertEquals("88e84a3cb5986ffec233920ee531639ba63bb117e13b1195e57eda030bbc4879",
                Hex.toHexString(resultDouble));
    }

}
