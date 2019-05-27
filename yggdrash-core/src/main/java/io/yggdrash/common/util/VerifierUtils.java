/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.common.util;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.trie.Trie;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBody;
import io.yggdrash.core.blockchain.TransactionHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;

import static io.yggdrash.common.config.Constants.Key.SIGNATURE;
import static io.yggdrash.common.config.Constants.SIGNATURE_LENGTH;
import static io.yggdrash.common.config.Constants.TIMESTAMP_2018;

public class VerifierUtils {

    private VerifierUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final Logger log = LoggerFactory.getLogger(VerifierUtils.class);

    public static boolean isGenesis(BlockHeader header) {
        return header.getIndex() == 0;
    }

    public static boolean verifyGenesisHash(Block block) {
        return block.getPrevBlockHash().equals(Sha3Hash.createByHashed(Constants.EMPTY_HASH));
    }

    public static boolean verify(Transaction transaction) {

        if (!verifyTimestamp(transaction)) {
            return false;
        }

        if (!verifyDataFormat(transaction)) {
            return false;
        }

        return verifySignature(transaction);
    }

    public static boolean verify(Block block) {

        if (isGenesis(block.getHeader())) {
            return true;
        }
        if (!verifyDataFormat(block)) {
            return false;
        }

        if (!verifyBlockBodyHash(block)) {
            return false;
        }

        return verifySignature(block);
    }

    public static boolean verifyBlockBodyHash(Block block) {
        List<Transaction> txsList = block.getBody().getTransactionList();
        byte[] merkleRoot = Trie.getMerkleRoot(txsList);
        return Arrays.equals(merkleRoot, block.getHeader().getMerkleRoot());
    }

    public static boolean verifyTimestamp(Transaction transaction) {
        return verifyTimestamp(transaction.getHeader().getTimestamp());
    }

    /**
     * The timestamp should be less than 1 hour compared with the current node time.
     *
     * @param timeStamp to be verified
     * @return boolean
     */
    private static boolean verifyTimestamp(Long timeStamp) {
        long hour = (1000 * 60 * 60);
        long curTime = System.currentTimeMillis();
        return timeStamp.compareTo(curTime + hour) < 0 && timeStamp.compareTo(curTime - hour) > 0;
    }

    public static boolean verifySignature(Transaction tx) {
        return verifySignature(tx.getSignature(), tx.getHeader().getHashForSigning());
    }

    public static boolean verifySignature(Block block) {
        return verifySignature(block.getSignature(), block.getHeader().getHashForSigning());
    }

    private static boolean verifySignature(byte[] protoSignature, byte[] hashedHeader) {
        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(protoSignature);
        ECKey ecKeyPub;

        try {
            ecKeyPub = ECKey.signatureToKey(hashedHeader, ecdsaSignature);
        } catch (SignatureException e) {
            return false; // handling exception
        }

        return ecKeyPub.verify(hashedHeader, ecdsaSignature);
    }

    /**
     * Verify a transaction about transaction format.
     *
     * @return true(success), false(fail)
     */
    public static boolean verifyDataFormat(Transaction tx) {
        TransactionHeader header = tx.getHeader();
        TransactionBody body = tx.getBody();

        // TODO CheckByValidate By Code
        boolean check = true;

        check &= verifyCheckLengthNotNull(
                header.getChain(), Constants.BRANCH_LENGTH, "chain");
        check &= verifyCheckLengthNotNull(
                header.getVersion(), TransactionHeader.VERSION_LENGTH, "version");
        check &= verifyCheckLengthNotNull(
                header.getType(), TransactionHeader.TYPE_LENGTH, "type");
        check &= verifyTimestampAfter2018(header.getTimestamp());
        check &= verifyCheckLengthNotNull(
                header.getBodyHash(), Constants.HASH_LENGTH, "bodyHash");
        check &= verifyBodyLength(header.getBodyLength(), body.getLength());
        check &= verifyCheckLengthNotNull(tx.getSignature(), Constants.SIGNATURE_LENGTH, SIGNATURE);

        // check bodyHash
        if (!Arrays.equals(header.getBodyHash(), HashUtil.sha3(body.toBinary()))) {
            String bodyHash = Hex.toHexString(header.getBodyHash());
            log.debug("bodyHash is not equal to body :{}", bodyHash);
            return false;
        }

        return check;
    }

    /**
     * Verify a block about block format.
     *
     * @return true(success), false(fail)
     */
    public static boolean verifyDataFormat(Block block) {
        BlockHeader header = block.getHeader();
        BlockBody body = block.getBody();

        // TODO CheckByValidate By Code
        boolean check = true;

        check &= verifyCheckLengthNotNull(
                header.getChain(), Constants.BRANCH_LENGTH, "chain");
        check &= verifyCheckLengthNotNull(
                header.getVersion(), BlockHeader.VERSION_LENGTH, "version");
        check &= verifyCheckLengthNotNull(header.getType(), BlockHeader.TYPE_LENGTH, "type");
        check &= verifyCheckLengthNotNull(
                header.getPrevBlockHash(), Constants.HASH_LENGTH, "prevBlockHash");
        check &= verifyCheckLengthNotNull(
                header.getMerkleRoot(), Constants.HASH_LENGTH, "merkleRootLength");
        if (isGenesis(header)) {
            // Genesis Block is not check signature
            check &= verifyCheckLengthNotNull(block.getSignature(), SIGNATURE_LENGTH, SIGNATURE);
        }
        check &= header.getIndex() >= 0;
        check &= verifyTimestampAfter2018(header.getTimestamp());
        check &= verifyBodyLength(header.getBodyLength(), body.getLength());
        check &= Arrays.equals(header.getMerkleRoot(), Trie.getMerkleRoot(body.getTransactionList()));

        return check;
    }

    private static boolean verifyCheckLengthNotNull(byte[] data, int length, String msg) {
        boolean result = !(data == null || data.length != length);

        if (!result) {
            log.debug("{} is not valid.", msg);
        }

        return result;
    }

    private static boolean verifyTimestampAfter2018(long timestamp) {
        return timestamp > TIMESTAMP_2018;
    }

    private static boolean verifyBodyLength(long length, long bodyLength) {
        return !(length < 0) || length != bodyLength;
    }
}
