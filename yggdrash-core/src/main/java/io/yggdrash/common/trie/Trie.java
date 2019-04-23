package io.yggdrash.common.trie;

import io.yggdrash.common.config.Constants;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.core.blockchain.Transaction;

import java.util.ArrayList;
import java.util.List;

import static io.yggdrash.common.crypto.HashUtil.HASH_256_ALGORITHM_NAME;

/**
 * Trie Class.
 * <br> referenced <a href="https://github.com/bitcoinj"> bitcoinj </a>
 */
public class Trie {

    private Trie() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Get merkleRoot using Transactions.
     *
     * @param txs Transaction list
     * @return merkle root value (byte[32]) <br>
     * null - if txs is null or txs.size is smaller than 1
     */
    public static byte[] getMerkleRoot(List<Transaction> txs) {

        if (txs == null || txs.isEmpty() || txs.contains(null)) {
            return Constants.EMPTY_HASH;
        }

        List<byte[]> tree = new ArrayList<>();
        for (Transaction tx : txs) {
            tree.add(tx.getHash().getBytes());
        }

        return getMerkleRoot(tree, HASH_256_ALGORITHM_NAME);
    }

    public static byte[] getMerkleRoot(List<byte[]> hashTree, String algorithm) {
        return getMerkleRoot(hashTree, algorithm, false);
    }

    /**
     * Get merkleRoot using list of hashed data.
     *
     * @param hashTree   ArrayList of hashed data
     * @param algorithm  Hash algorithm
     * @param doubleHash Whether double hash or not
     * @return merkle root data
     */
    public static byte[] getMerkleRoot(List<byte[]> hashTree, String algorithm, boolean doubleHash) {

        int treeSize;
        int levelOffset = 0;

        try {
            if (hashTree == null || hashTree.contains(null)) {
                return Constants.EMPTY_HASH;
            }

            treeSize = hashTree.size();
            if (treeSize == 0) {
                return Constants.EMPTY_HASH;
            }
        } catch (Exception e) {
            return Constants.EMPTY_HASH;
        }

        for (int levelSize = treeSize; levelSize > 1; levelSize = (levelSize + 1) / 2) {
            for (int left = 0; left < levelSize; left += 2) {
                int right = Math.min(left + 1, levelSize - 1);
                byte[] leftBytes = hashTree.get(levelOffset + left);
                byte[] rightBytes = hashTree.get(levelOffset + right);
                hashTree.add(HashUtil.hash(
                        ByteUtil.merge(leftBytes, rightBytes), algorithm, doubleHash));
            }

            levelOffset += levelSize;
        }

        return hashTree.get(hashTree.size() - 1);
    }
}
