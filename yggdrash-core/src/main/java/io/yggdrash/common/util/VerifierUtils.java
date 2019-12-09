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

import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.contract.ContractVersion;
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
            log.debug("verify Fail Time stamp");
            return false;
        }

        if (!verifyDataFormat(transaction)) {
            log.debug("verify Fail Data Format");
            return false;
        }

        if (!verifySignature(transaction)) {
            log.debug("verifySignature() is failed.");
            return false;
        }

        return true;
    }

    public static boolean verify(Block block) {

        if (isGenesis(block.getHeader())) {
            return true;
        }
        if (!verifyDataFormat(block)) {
            log.debug("verifyDataFormat() is failed.");
            return false;
        }

        if (!verifyBlockBodyHash(block)) {
            log.debug("verifyBlockBodyHash() is failed.");
            return false;
        }

        if (!verifySignature(block)) {
            log.debug("verifySignature() is failed.");
            return false;
        }

        return true;
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
        long twoHour = 2 * (1000 * 60 * 60);
        long curTime = System.currentTimeMillis();
        return timeStamp.compareTo(curTime + twoHour) < 0 && timeStamp.compareTo(curTime - twoHour) > 0;
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
        return verifyDataFormatCode(tx) == 0;
    }

    /**
     * Verify a transaction about transaction format.
     *
     * @return true(success), false(fail)
     */
    public static int verifyDataFormatCode(Transaction tx) {
        int verifyCode = 0;

        TransactionHeader header = tx.getHeader();

        verifyCode |= verifyCheckLengthNotNull(
                header.getChain(), Constants.BRANCH_LENGTH, "chain") ? 0x00 : 0x01;
        verifyCode |= verifyCheckLengthNotNull(
                header.getVersion(), TransactionHeader.VERSION_LENGTH, "version") ? 0x00 : 0x02;
        verifyCode |= verifyCheckLengthNotNull(
                header.getType(), TransactionHeader.TYPE_LENGTH, "type") ? 0x00 : 0x04;
        verifyCode |= verifyTimestampAfter2018(header.getTimestamp()) ? 0x00 : 0x08;
        verifyCode |= verifyCheckLengthNotNull(
                header.getBodyHash(), Constants.HASH_LENGTH, "bodyHash") ? 0x00 : 0x10;

        TransactionBody body = tx.getTransactionBody();
        verifyCode |= verifyBodyLength(header.getBodyLength(), body.getLength()) ? 0x00 : 0x20;
        verifyCode |= verifyTxBodyFormat(body) ? 0x00 : 0x40;

        verifyCode |= verifyCheckLengthNotNull(tx.getSignature(), Constants.SIGNATURE_LENGTH, SIGNATURE) ? 0x00 : 0x80;

        // check bodyHash
        if (!Arrays.equals(header.getBodyHash(), HashUtil.sha3(body.toBinary()))) {
            String bodyHash = Hex.toHexString(header.getBodyHash());
            log.debug("bodyHash is not equal to body :{}", bodyHash);
            verifyCode |= 0x100;
        }

        log.trace("Transaction Verify CODE : {}", verifyCode);

        return verifyCode;

    }


    /**
     * Verify a block about block format.
     *
     * @return true(success), false(fail)
     */
    public static boolean verifyDataFormat(Block block) {
        BlockHeader header = block.getHeader();

        // TODO CheckByValidate By Code
        boolean check = verifyCheckLengthNotNull(
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

        BlockBody body = block.getBody();
        check &= verifyBodyLength(header.getBodyLength(), body.getLength());
        check &= Arrays.equals(header.getMerkleRoot(), Trie.getMerkleRoot(body.getTransactionList()));

        return check;
    }

    // TxBody format has not been fixed yet. The following validation is required until the TxBody is fixed.
    public static boolean verifyTxBodyFormat(TransactionBody txBody) {
        JsonObject body = txBody.getBody();

        // check body size
        if (body.size() != 3) {
            log.debug("Verify txBody format : The body size should be 3, body size = {}", body.size());
            return false;
        }

        // check keys exists
        if (!body.has("contractVersion")) {
            log.debug("Verify txBody format : The body has no 'contractVersion' key");
            return false;
        }
        if (!body.has("method")) {
            log.debug("Verify txBody format : The body has no 'method' key");
            return false;
        }
        if (!body.has("params")) {
            log.debug("Verify txBody format : The body has no 'params' key");
            return false;
        }

        // check value types
        if (!body.get("contractVersion").isJsonPrimitive()
                || !body.get("contractVersion").getAsJsonPrimitive().isString()) {
            log.debug("Verify txBody format : The value type of the 'contractVersion' must be a String.");
            return false;
        }
        if (!body.get("method").isJsonPrimitive() || !body.get("method").getAsJsonPrimitive().isString()) {
            log.debug("Verify txBody format : The value type of the 'method' must be a String.");
            return false;
        }

        if (!body.get("params").isJsonObject()) {
            log.debug("Verify txBody format : The value type of the 'params' must be a JsonObject.");
            return false;
        }

        // contractVersion length check
        return verifyCheckLengthNotNull(ContractVersion.of(body.get("contractVersion").getAsString()).getBytes(),
                Constants.CONTRACT_VERSION_LENGTH, "contractVersion");
    }

    private static boolean verifyCheckLengthNotNull(byte[] data, int length, String msg) {
        boolean result = !(data == null || data.length != length);

        if (!result) {
            log.debug("Verify length and null : {} is not valid.", msg);
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
