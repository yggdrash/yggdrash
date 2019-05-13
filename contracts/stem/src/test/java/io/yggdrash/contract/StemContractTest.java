/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.common.contract.vo.PrefixKeyEnum;
import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.BranchUtil;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.common.utils.ContractUtils;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptImpl;
import io.yggdrash.contract.core.annotation.ContractBranchStateStore;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.core.wallet.AesEncrypt;
import io.yggdrash.core.wallet.Wallet;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StemContractTest {
    private static final Logger log = LoggerFactory.getLogger(StemContractTest.class);
    private static final StemContract.StemService stemContract = new StemContract.StemService();

    private static final File branchFile = new File("../../yggdrash-core/src/main/resources/branch-yggdrash.json");

    private Field txReceiptField;
    TestYeed testYeed = new TestYeed();

    JsonObject branchSample;
    String branchId;
    StateStore stateStore;

    @Before
    public void setUp() throws IllegalAccessException, IOException {
        // Steup StemContract
        stateStore = new StateStore(new HashMapDbSource());

        List<Field> txReceipt = ContractUtils.txReceiptFields(stemContract);
        if (txReceipt.size() == 1) {
            txReceiptField = txReceipt.get(0);
        }
        for (Field f : ContractUtils.contractFields(stemContract, ContractStateStore.class)) {
            f.setAccessible(true);
            f.set(stemContract, stateStore);
        }
        // 1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e
        JsonObject obj = new JsonObject();
        obj.addProperty("address", "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e");
        assertTrue("setup", BigInteger.ZERO.compareTo(testYeed.balanceOf(obj)) < 0);

        TestBranchStateStore branchStateStore = new TestBranchStateStore();
        branchStateStore.getValidators().getValidatorMap()
                .put("81b7e08f65bdf5648606c89998a9cc8164397647",
                        new Validator("81b7e08f65bdf5648606c89998a9cc8164397647"));

        for (Field f : ContractUtils.contractFields(stemContract, ContractBranchStateStore.class)) {
            f.setAccessible(true);
            f.set(stemContract, branchStateStore);
        }

        try (InputStream is = new FileInputStream(branchFile)) {
            String branchString = IOUtils.toString(is, FileUtil.DEFAULT_CHARSET);
            branchSample = JsonUtil.parseJsonObject(branchString);
        }
        // branch Id generator to util
        byte[] rawBranchId = BranchUtil.branchIdGenerator(branchSample);
        branchId = HexUtil.toHexString(rawBranchId);
        log.debug("Branch Id : {}", branchId);
    }

    @Test
    public void createStemBranch() {
        // Set Receipt
        TransactionReceipt receipt = createReceipt();
        setUpReceipt(receipt);

        // add params
        JsonObject param = new JsonObject();
        // TODO param add branch and fee
        // Get Branch sample in resources

        param.add("branch", branchSample);
        param.addProperty("fee", BigInteger.valueOf(1000000));


        stemContract.create(param);
        assertTrue("Branch Create Success", receipt.isSuccess());
        
        String branchKey = String.format("%s%s", PrefixKeyEnum.STEM_BRANCH, branchId);

        receipt.getTxLog().stream().forEach(l -> log.debug(l));

        assertTrue("Branch Stored", stateStore.contains(branchKey));

        String branchMetaKey = String.format("%s%s", PrefixKeyEnum.STEM_META, branchId);
        assertTrue("Branch Meta Stored", stateStore.contains(branchMetaKey));
    }

    @Test
    public void getBranchQuery() {
        createStemBranch();

        JsonObject param = new JsonObject();
        param.addProperty("branchId", branchId);


        JsonObject branch = stemContract.getBranch(param);

        byte[] rawBranchId = BranchUtil.branchIdGenerator(branch);
        String queryBranchId = HexUtil.toHexString(rawBranchId);
        assertEquals("branch Id check", branchId, queryBranchId);
    }

    @Test
    public void getContractQuery() {
        createStemBranch();

        JsonObject param = new JsonObject();
        param.addProperty("branchId", branchId);

        Set<JsonObject> contracts = stemContract.getContract(param);
        contracts.stream()
                .forEach(c -> log.debug(c.getAsJsonObject().get("contractVersion").getAsString()));
        assertTrue("Contract Size", contracts.size() == 3);
    }

    @Test
    public void updateBranchMetaInformation() {
        // all meta information is not delete by transaction
        createStemBranch();
        // Set new Receipt
        TransactionReceipt receipt = createReceipt();
        receipt.setIssuer("101167aaf090581b91c08480f6e559acdd9a3ddd");
        setUpReceipt(receipt);

        JsonObject branchUpdate = new JsonObject();
        branchUpdate.addProperty("name", "NOT UPDATE");
        branchUpdate.addProperty("description", "UPDATE DESCRIPTION");


        JsonObject param = new JsonObject();
        param.addProperty("branchId", branchId);
        param.add("branch", branchUpdate);
        param.addProperty("fee", BigInteger.valueOf(1000000));

        stemContract.update(param);

        assertEquals("update result", receipt.getStatus(), ExecuteStatus.SUCCESS);
        JsonObject metaInfo = stemContract.getBranchMeta(param);

        assertEquals("name did not update meta information", metaInfo.get("name").getAsString(), "YGGDRASH");
        assertEquals("description is updated", metaInfo.get("description").getAsString(), "UPDATE DESCRIPTION");
    }

    @Test
    public void updateNotExistBranch() {

        TransactionReceipt receipt = createReceipt();
        setUpReceipt(receipt);
        JsonObject branchUpdate = new JsonObject();
        branchUpdate.addProperty("name", "NOT UPDATE");
        branchUpdate.addProperty("description", "UPDATE DESCRIPTION");


        JsonObject param = new JsonObject();
        param.addProperty("branchId", branchId);
        param.add("branch", branchUpdate);
        param.addProperty("fee", BigInteger.valueOf(1000000));

        stemContract.update(param);

        assertEquals("transaction update is False", receipt.getStatus(), ExecuteStatus.FALSE);
    }

    @Test
    public void metaDataMerge() {

        JsonObject metaSample = new JsonObject();
        metaSample.addProperty("name", "YGGDRASH");
        metaSample.addProperty("symbol", "YGGDRASH");
        metaSample.addProperty("property", "platform");
        metaSample.addProperty("description", "TRUST-based Multi-dimensional Blockchains");


        JsonObject metaUpdate = new JsonObject();
        metaUpdate.addProperty("name", "NOT UPDATE");
        metaUpdate.addProperty("symbol", "NOT UPDATE");
        metaUpdate.addProperty("description", "UPDATE DESCRIPTION");

        JsonObject metaUpdated = stemContract.metaMerge(metaSample, metaUpdate);


        assertEquals("Name is not update",
                metaUpdated.get("name").getAsString(), "YGGDRASH");
        assertEquals("Symbol is not update",
                metaUpdated.get("symbol").getAsString(), "YGGDRASH");
        assertEquals("Property is not update",
                metaUpdated.get("property").getAsString(), "platform");
        assertEquals("description is Update",
                metaUpdated.get("description").getAsString(), "UPDATE DESCRIPTION");


    }

    @Test
    public void otherBranchCreate() {
        createStemBranch();

        TransactionReceipt receipt = createReceipt();
        setUpReceipt(receipt);

        JsonObject otherBranch = branchSample.deepCopy();
        otherBranch.addProperty("name", "ETH TO YEED Branch");
        otherBranch.addProperty("symbol", "ETY");
        otherBranch.addProperty("property", "exchange");
        otherBranch.addProperty("timeStamp", "00000166c837f0c9");


        JsonObject param = new JsonObject();
        param.add("branch", otherBranch);
        param.addProperty("fee", BigInteger.valueOf(10000));

        byte[] rawBranchId = BranchUtil.branchIdGenerator(otherBranch);
        String otherBranchId = HexUtil.toHexString(rawBranchId);

        JsonObject queryParam = new JsonObject();
        queryParam.addProperty("branchId", otherBranchId);

        stemContract.create(param);

        assertEquals("otherBranch Create", receipt.getStatus(), ExecuteStatus.SUCCESS);

        JsonObject queryMeta = stemContract.getBranchMeta(queryParam);

        assertEquals("otehr branch symbol", "ETY", queryMeta.get("symbol").getAsString());
    }

    @Test
    public void updateValidators() throws IOException, InvalidCipherTextException {
        // TODO validator can suggest validator and other validator vote to suggest validator
        // TODO all message is just one transaction

        InputStream testUpdateBranch = getClass().getClassLoader().getResourceAsStream("update-branch.json");
        String branchString = IOUtils.toString(testUpdateBranch, FileUtil.DEFAULT_CHARSET);
        JsonObject updateBranchObject = JsonUtil.parseJsonObject(branchString);
        byte[] rawBranchId = BranchUtil.branchIdGenerator(updateBranchObject);
        String updateBranchId = HexUtil.toHexString(rawBranchId);

        String proposer = "5244d8163ea6fdd62aa08ae878b084faa0b013be";

        TransactionReceipt receipt = createReceipt();
        receipt.setIssuer(proposer);
        setUpReceipt(receipt);


        JsonObject param = new JsonObject();
        // Get Branch sample in resources

        param.add("branch", updateBranchObject);
        param.addProperty("fee", BigInteger.valueOf(1000000));

        stemContract.create(param);
        assertTrue("Branch Create Success", receipt.isSuccess());

        // message property
        // BRANCH_ID , (20byte)
        // BLOCK_HEIGHT (8 BYTE),
        // PROPOSER_VALIDATOR (20 byte)
        // TARGET_VALIDATOR (20 byte)
        // OPERATING_FLAG (1byte)
        // SIGNATURE (65 byte) ( N Validators )

        String targetBranchId = updateBranchId;
        Long blockHeight = 100L;

        String targetValidator = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        String operatingFlag = "1"; // ADD OR DELETE

        byte[] message = ByteUtil.merge(
                HexUtil.hexStringToBytes(targetBranchId),
                ByteUtil.longToBytes(blockHeight),
                HexUtil.hexStringToBytes(proposer),
                HexUtil.hexStringToBytes(targetValidator),
                operatingFlag.getBytes()
        );
        message = HashUtil.sha3(message);
        log.debug("message Size : {} ", message.length);
        assertEquals("message Length : ", message.length, 32);

        log.debug(updateBranchId);

        String v1Path = getClass()
                .getResource("/validatorKeys/5244d8163ea6fdd62aa08ae878b084faa0b013be.json").getFile();

        log.debug(v1Path);
        File f = new File(v1Path);

        TestWallet v1 = new TestWallet(f, "Aa1234567890!");

        byte[] signV1 = v1.sign(message, true);
        log.debug(HexUtil.toHexString(signV1));
        assertTrue("verify : ", v1.verify(message, signV1, true));

        byte[] validator1Message = ByteUtil.merge(message, signV1);

        String v2Path = getClass()
                .getResource("/validatorKeys/a4e063d728ee7a45c5bab3aa2283822d49a9f73a.json").getFile();
        TestWallet v2 = new TestWallet(new File(v2Path), "Password1234!");
        byte[] signV2 = v2.sign(message, true);

        byte[] validator2Message = ByteUtil.merge(message, signV2);

        String v3Path = getClass()
                .getResource("/validatorKeys/e38f532685b5e61eca5bc25a0da8ea87d74e671e.json").getFile();
        TestWallet v3 = new TestWallet(new File(v3Path), "Password1234!");
        byte[] signV3 = v3.sign(message, true);

        byte[] validator3Message = ByteUtil.merge(message, signV3);

        String v4Path = getClass()
                .getResource("/validatorKeys/f5927c28b66d4bb4b50395662a097370e8cd7e58.json").getFile();
        TestWallet v4 = new TestWallet(new File(v4Path), "Password1234!");
        byte[] signV4 = v4.sign(message, true);

        byte[] validator4Message = ByteUtil.merge(message, signV4);

        String[] signed = new String[]{HexUtil.toHexString(signV1), HexUtil.toHexString(signV2),
                HexUtil.toHexString(signV3), HexUtil.toHexString(signV4)};

        JsonArray signedArray = new JsonArray();
        Arrays.stream(signed).forEach(sg -> signedArray.add(sg));

        JsonObject updateBranchValiator = new JsonObject();
        updateBranchValiator.addProperty("branchId", targetBranchId);
        updateBranchValiator.addProperty("blockHeight", blockHeight);
        updateBranchValiator.addProperty("proposer", proposer);
        updateBranchValiator.addProperty("blockHeight", blockHeight);
        updateBranchValiator.addProperty("targetValidator", targetValidator);
        updateBranchValiator.addProperty("operatingFlag", operatingFlag);
        updateBranchValiator.add("signed", signedArray);

        log.debug(JsonUtil.prettyFormat(updateBranchValiator));
        stemContract.updateValidator(updateBranchValiator);

        // Test Sign verify
        signV1[0] = (byte)(signV1[0] - 27);
        ECKey.ECDSASignature signature = new ECKey.ECDSASignature(signV1);
        int realV = signV1[0];
        log.debug("realV {}", realV);
        byte[] addressV1 = ECKey.recoverAddressFromSignature(signature.v, signature, message);
        log.debug("address 1 {}" , HexUtil.toHexString(addressV1));

        assertTrue("address Is equal", Arrays.equals(addressV1, v1.address));
    }

    private TransactionReceipt createReceipt() {
        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setIssuer("101167aaf090581b91c08480f6e559acdd9a3ddd");
        return receipt;
    }

    private void setUpReceipt(TransactionReceipt receipt) {
        try {
            txReceiptField.set(stemContract, receipt);
            testYeed.setTxReceipt(receipt);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public class TestWallet {

        private static final String WALLET_PBKDF2_NAME = "pbkdf2";
        private static final int WALLET_PBKDF2_ITERATION = 262144;
        private static final int WALLET_PBKDF2_DKLEN = 32;
        private static final String WALLET_PBKDF2_PRF = "hmac-sha256";
        private static final String WALLET_PBKDF2_HMAC_HASH = "KECCAK-256";
        private static final String WALLET_PBKDF2_ALGORITHM = "SHA-256";
        private static final String WALLET_KEY_ENCRYPT_ALGORITHM = "aes-128-cbc";

        private ECKey key;
        private String keyPath;
        private String keyName;
        private byte[] address;
        private byte[] publicKey;

        public TestWallet(File file, String password) throws IOException, InvalidCipherTextException {
            decryptKeyFileInit(file, password);

        }

        public byte[] sign(byte[] data, boolean hashed) {
            ECKey.ECDSASignature signature = null;
            if (hashed) {
                signature = key.sign(data);
            } else {
                signature = key.sign(HashUtil.sha3(data));
            }

            log.debug("V value {}", (int)signature.v);

            return signature.toBinary();

        }

        private void decryptKeyFileInit(File keyFile, String password)
            throws IOException, InvalidCipherTextException {
            String json = FileUtil.readFileToString(keyFile, FileUtil.DEFAULT_CHARSET);
            JsonObject keyJsonObject = JsonUtil.parseJsonObject(json);

            byte[] salt = Hex.decode(getCryptoJsonObect(keyJsonObject)
                    .getAsJsonObject("kdfparams")
                    .get("salt")
                    .getAsString());
            byte[] kdfPass = HashUtil.pbkdf2(
                    password.getBytes(),
                    salt,
                    WALLET_PBKDF2_ITERATION,
                    WALLET_PBKDF2_DKLEN,
                    WALLET_PBKDF2_ALGORITHM);
            byte[] encData = Hex.decode(getCryptoJsonObect(keyJsonObject)
                    .get("ciphertext")
                    .getAsString());

            byte[] newMac = HashUtil.hash(
                    ByteUtil.merge(ByteUtil.parseBytes(kdfPass, 16, 16), encData),
                    WALLET_PBKDF2_HMAC_HASH);
            byte[] mac = Hex.decode(getCryptoJsonObect(keyJsonObject)
                    .get("mac")
                    .getAsString());
            if (!Arrays.equals(newMac, mac)) {
                throw new InvalidCipherTextException("mac is not valid");
            }

            byte[] iv = Hex.decode(getCryptoJsonObect(keyJsonObject)
                    .getAsJsonObject("cipherparams")
                    .get("iv")
                    .getAsString());

            byte[] priKey = AesEncrypt.decrypt(
                    encData, ByteUtil.parseBytes(kdfPass, 0, 16), iv);
            this.key = ECKey.fromPrivate(priKey);
            this.keyPath = keyPath;
            this.keyName = keyName;
            this.address = key.getAddress();
            this.publicKey = key.getPubKey();
        }

        private JsonObject getCryptoJsonObect(JsonObject keyJsonObject) {
            return keyJsonObject.getAsJsonObject("crypto");
        }

        public boolean verify(byte[] data, byte[] signature, boolean isHashed) {
            ECKey.ECDSASignature sig = new ECKey.ECDSASignature(signature);
            if (isHashed) {
                return key.verify(data, sig);
            } else {
                return key.verify(HashUtil.sha3(data), sig);
            }

        }
    }

}