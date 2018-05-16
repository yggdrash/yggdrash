package io.yggdrash.trie;

import io.yggdrash.core.Transaction;
import io.yggdrash.util.HashUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Trie Class
 */
public class Trie {

    /**
     * Get merkle root vaule
     * @param txs Transaction list
     * @return
     * byte[32] - merkle root value <br>
     * null - if txs is null or txs.size is smaller than 1
     */
    public static byte[] getMerkleRoot(List<Transaction> txs) {

        if(txs == null || txs.size() < 1)
            return null;

        ArrayList<byte[]> tree = new ArrayList<>();
        for (Transaction tx : txs) {
            tree.add(tx.getHash());
        }

        int levelOffset = 0;
        for (int levelSize = txs.size(); levelSize > 1; levelSize = (levelSize + 1) / 2) {

            for (int left = 0; left < levelSize; left += 2) {
                int right = Math.min(left + 1, levelSize - 1);
                byte[] leftBytes = reverseBytes(tree.get(levelOffset + left));
                byte[] rightBytes = reverseBytes(tree.get(levelOffset + right));
                tree.add(reverseBytes(hashTwice(leftBytes, 0, 32, rightBytes, 0, 32)));
            }
            levelOffset += levelSize;
        }

        return HashUtils.sha256(tree.get(tree.size()-1));
    }

    private static byte[] reverseBytes(byte[] bytes) {
        byte[] buf = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            buf[i] = bytes[bytes.length - 1 - i];
        return buf;
    }

    private static byte[] hashTwice(byte[] input1, int offset1, int length1,
                                   byte[] input2, int offset2, int length2) {
        MessageDigest digest = newDigest();
        digest.update(input1, offset1, length1);
        digest.update(input2, offset2, length2);
        return digest.digest(digest.digest());
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
