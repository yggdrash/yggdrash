package io.yggdrash.common.trie;

import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionHusk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Trie Class.
 * <br> referenced <a href="https://github.com/bitcoinj"> bitcoinj </a>
 */
public class Trie {

    /**
     * Get merkle root value.
     *
     * @param txs Transaction list
     * @return byte[32] - merkle root value <br>
     * null - if txs is null or txs.size is smaller than 1
     */
    public static byte[] getMerkleRootHusk(List<TransactionHusk> txs) {

        if (txs == null || txs.size() < 1 || txs.contains(null)) {
            return null;
        }

        ArrayList<byte[]> tree = new ArrayList<>();
        for (TransactionHusk tx : txs) {
            tree.add(tx.getHash().getBytes());
        }

        return getMerkleRoot(tree);
    }

    /**
     * Get merkleRoot using Transactions.
     *
     * @param txs Transaction list
     * @return merkle root value (byte[32]) <br>
     * null - if txs is null or txs.size is smaller than 1
     */
    public static byte[] getMerkleRoot(List<Transaction> txs) {

        if (txs == null || txs.size() < 1 || txs.contains(null)) {
            return null;
        }

        ArrayList<byte[]> tree = new ArrayList<>();
        for (Transaction tx : txs) {
            tree.add(tx.getHash());
        }

        return getMerkleRoot(tree);
    }

    /**
     * Get merkleRoot using list of hashed data.
     *
     * @param hashTree   ArrayList of hashed data
     * @param algorithm  Hash algorithm
     * @param doubleHash Whether double hash or not
     * @return merkle root data
     */
    public static byte[] getMerkleRoot(
            ArrayList<byte[]> hashTree, String algorithm, boolean doubleHash) {

        int treeSize = 0;
        int levelOffset = 0;

        try {
            if (hashTree == null || hashTree.contains(null)) {
                return null;
            }

            treeSize = hashTree.size();
            if (treeSize == 0) {
                return null;
            }
        } catch (Exception e) {
            return null;
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

    public static byte[] getMerkleRoot(ArrayList<byte[]> hashTree, String algorithm) {
        return getMerkleRoot(hashTree, algorithm, false);
    }

    public static byte[] getMerkleRoot(ArrayList<byte[]> hashTree) {
        return getMerkleRoot(hashTree, "KECCAK-256");
    }

}
