/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.utils.SerializationUtil;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.ContractConstants;
import io.yggdrash.core.blockchain.osgi.ContractManager;
import io.yggdrash.core.blockchain.osgi.ContractManagerBuilder;
import io.yggdrash.core.blockchain.osgi.framework.BootFrameworkConfig;
import io.yggdrash.core.blockchain.osgi.framework.BootFrameworkLauncher;
import io.yggdrash.core.blockchain.osgi.framework.BundleService;
import io.yggdrash.core.blockchain.osgi.framework.BundleServiceImpl;
import io.yggdrash.core.blockchain.osgi.framework.FrameworkConfig;
import io.yggdrash.core.blockchain.osgi.framework.FrameworkLauncher;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.runtime.result.BlockRuntimeResult;
import io.yggdrash.core.store.BlockChainStore;
import io.yggdrash.core.store.BlockChainStoreBuilder;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.core.store.PbftBlockStoreMock;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.mock.ContractMock;
import io.yggdrash.proto.PbftProto;
import org.apache.commons.codec.binary.Base64;
import org.osgi.framework.Bundle;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.yggdrash.common.config.Constants.BASE_CURRENCY;

public class ContractTestUtils {

    public static Map<ContractManager, ContractStore> createContractManager(GenesisBlock genesis) {
        DefaultConfig config = new DefaultConfig();
        BlockChainStore bcStore = BlockChainStoreBuilder.newBuilder(genesis.getBranchId())
                .withDataBasePath(config.getDatabasePath())
                .withProductionMode(config.isProductionMode())
                .setConsensusAlgorithm(null)
                .setBlockStoreFactory(PbftBlockStoreMock::new)
                .build();
        ContractStore contractStore = bcStore.getContractStore();
        FrameworkConfig bootFrameworkConfig = new BootFrameworkConfig(config, genesis.getBranchId());
        FrameworkLauncher bootFrameworkLauncher = new BootFrameworkLauncher(bootFrameworkConfig);
        BundleService bundleService = new BundleServiceImpl(bootFrameworkLauncher.getBundleContext());

        SystemProperties systemProperties = BlockChainTestUtils.createDefaultSystemProperties();

        ContractManager manager = ContractManagerBuilder.newInstance()
                .withGenesis(genesis)
                .withBundleManager(bundleService)
                .withDefaultConfig(config)
                .withContractStore(contractStore)
                .withLogStore(bcStore.getLogStore())
                .withSystemProperties(systemProperties)
                .build();

        Map<ContractManager, ContractStore> res = new HashMap<>();
        res.put(manager, contractStore);

        return res;
    }

    public static Sha3Hash calStateRoot(ContractManager contractManager, List<Transaction> txs) {
        if (txs.size() > 0) {
            BlockRuntimeResult executePendingTxs = contractManager.executeTxs(txs);
            return executePendingTxs.getBlockResult().containsKey("stateRoot")
                    ? new Sha3Hash(executePendingTxs.getBlockResult().get("stateRoot").get("stateHash").getAsString())
                    : contractManager.getOriginStateRootHash();
        } else {
            return contractManager.getOriginStateRootHash();
        }
    }

    public static Sha3Hash calGenesisStateRoot(GenesisBlock genesis) {
        Map<ContractManager, ContractStore> map = createContractManager(genesis);
        ContractManager contractManager =  map.keySet().stream().findFirst().get();

        Sha3Hash genesisStateRootHash;
        if (genesis.getContractTxs().size() > 0) {
            genesisStateRootHash = new Sha3Hash(contractManager.executeTxs(genesis.getContractTxs())
                    .getBlockResult().get("stateRoot").get("stateHash").getAsString());
        } else {
            genesisStateRootHash = new Sha3Hash(Constants.EMPTY_HASH);
        }
        return genesisStateRootHash;

    }

    public static Sha3Hash calStateRoot(ConsensusBlock prevBlock, List<Transaction> txs) {
        Map<ContractManager, ContractStore> map = createContractManager(BlockChainTestUtils.getGenesis());
        ContractManager contractManager =  map.keySet().stream().findFirst().get();

        Map<String, Validator> validatorMap = new HashMap<>();
        validatorMap.put("TEST1",
                new Validator("a2b0f5fce600eb6c595b28d6253bed92be0568ed"));
        validatorMap.put("TEST2",
                new Validator("a2b0f5fce600eb6c595b28d6253bed92be0568ed"));
        validatorMap.put("TEST3",
                new Validator("a2b0f5fce600eb6c595b28d6253bed92be0568ed"));
        ValidatorSet validatorSet = new ValidatorSet();
        validatorSet.setValidatorMap(validatorMap);

        ContractStore contractStore = map.get(contractManager);
        contractStore.getBranchStore().setValidators(validatorSet);

        BlockRuntimeResult genesisResult = contractManager.executeTxs(BlockChainTestUtils.genesisBlock());
        contractManager.commitBlockResult(genesisResult);

        if (prevBlock.getHeader().getIndex() != 0L) {
            byte[] prevStateRoot = prevBlock.getHeader().getStateRoot();
            contractStore.getStateStore().put("stateRoot", createStateHashObj(prevStateRoot));
        }

        BlockRuntimeResult result = contractManager.executeTxs(txs);
        contractManager.commitBlockResult(result);
        return new Sha3Hash(result.getBlockResult().get("stateRoot").get("stateHash").getAsString());
    }

    private static JsonObject createStateHashObj(byte[] stateRootBytes) {
        Sha3Hash prevStateRootHash = new Sha3Hash(stateRootBytes, true);
        JsonObject stateHash = new JsonObject();
        stateHash.addProperty("stateHash", prevStateRootHash.toString());
        return  stateHash;
    }

    public static String setNamespace(ContractManager manager, ContractVersion contractVersion) {
        Bundle bundle = manager.getBundle(contractVersion);
        String name = bundle.getSymbolicName();
        byte[] bundleSymbolicSha3 = HashUtil.sha3omit12(name.getBytes());
        return new String(Base64.encodeBase64(bundleSymbolicSha3));
    }

    public static GenesisBlock createGenesis(String branchJson) throws IOException {
        return GenesisBlock.of(new FileInputStream(getFileFromResource(branchJson)));
    }

    public static ConsensusBlock<PbftProto.PbftBlock> createGenesisBlock(String branchJson) {
        return BlockChainTestUtils.genesisBlock(getFileFromResource(branchJson));
    }

    private static File getFileFromResource(String branchJson) {
        String filePath = Objects.requireNonNull(
                ContractTestUtils.class.getClassLoader().getResource(branchJson)).getFile();
        return new File(filePath);
    }

    public static Wallet createTestWallet(String keyStore) throws IOException, InvalidCipherTextException {
        String path = Objects.requireNonNull(ContractTestUtils.class.getClassLoader()
                .getResource(String.format("keys/%s", keyStore))).getPath();
        return new Wallet(path, "Aa1234567890!");
    }

    public static JsonObject contractProposeTxBodyJson(String contractVersion, String proposalType) {
        return nodeContractTxBodJson("propose", contractProposeParam(contractVersion, proposalType));
    }

    public static JsonObject contractVoteTxBodyJson(String txId, boolean agree) {
        return nodeContractTxBodJson("vote", contractVoteParam(txId, agree));
    }

    private static JsonObject nodeContractTxBodJson(String method, JsonObject params) {
        JsonObject txBody = new JsonObject();
        txBody.addProperty("contractVersion", ContractConstants.VERSIONING_CONTRACT.toString());
        txBody.addProperty("method", method);
        txBody.add("params", params);

        return txBody;
    }

    private static JsonObject contractProposeParam(String contractVersion, String proposalType) {
        JsonObject param = new JsonObject();
        param.addProperty("proposalVersion", contractVersion);
        param.addProperty("sourceUrl", "https://github.com/yggdrash/yggdrash");
        param.addProperty("buildVersion", "1.8.0_172");
        param.addProperty("proposalType", proposalType);

        return param;
    }

    private static JsonObject contractVoteParam(String txId, boolean agree) {
        JsonObject param = new JsonObject();
        param.addProperty("txId", txId);
        param.addProperty("agree", agree);

        return param;
    }

    public static JsonObject createParams(String key, String value) {
        JsonObject params = new JsonObject();
        params.addProperty(key, value);
        return params;
    }

    public static JsonObject transferTxBodyJson(String to, BigInteger amount) {
        JsonObject params = new JsonObject();
        params.addProperty("to", to);
        params.addProperty("amount", amount);
        params.addProperty("fee", BASE_CURRENCY.divide(BigInteger.valueOf(50L)));
        TestConstants.yggdrash();
        return txBodyJson(TestConstants.YEED_CONTRACT, "transfer", params, true);
    }

    public static JsonObject transferTxBodyJson(String to, BigInteger amount, ContractVersion contractVersion) {
        JsonObject params = new JsonObject();
        params.addProperty("to", to);
        params.addProperty("amount", amount);
        params.addProperty("fee", BASE_CURRENCY.divide(BigInteger.valueOf(50L)));
        TestConstants.yggdrash();
        return txBodyJson(contractVersion, "transfer", params, true);
    }

    public static JsonObject invalidMethodTransferTxBodyJson(String to, BigInteger amount) {
        JsonObject params = new JsonObject();
        params.addProperty("to", to);
        params.addProperty("amount", amount);
        params.addProperty("fee", BASE_CURRENCY.divide(BigInteger.valueOf(50L)));
        return txBodyJson(TestConstants.YEED_CONTRACT, "create", params, true);
    }

    public static JsonObject createTxBodyJson(JsonObject branch) {
        return stemTxBodyJson("create", branch);
    }

    public static JsonObject updateTxBodyJson(JsonObject branch) {
        return stemTxBodyJson("update", branch);
    }

    private static JsonObject stemTxBodyJson(String method, JsonObject branch) {
        TestConstants.yggdrash();
        return txBodyJson(TestConstants.STEM_CONTRACT, method, branch, false);
    }

    public static JsonObject txBodyJson(
            ContractVersion contractVersion, String method, JsonObject params, boolean isSystem) {
        JsonObject txBody = new JsonObject();
        txBody.addProperty("contractVersion", contractVersion.toString());
        txBody.addProperty("method", method);
        txBody.add("params", params);

        return txBody;
    }

    public static Branch createBranch(ContractMock contractMock) {
        JsonObject branchJson = contractMock.mock();
        return Branch.of(branchJson);
    }

    public static JsonObject createParamForCreateBranch(JsonObject branchObj) {
        JsonObject param = new JsonObject();
        param.add("branch", branchObj);
        param.addProperty("serviceFee", BigInteger.valueOf(100));
        return param;
    }

    public static JsonObject createSampleBranchJson() {
        TestConstants.yggdrash();

        final String name = "STEM";
        final String symbol = "STEM";
        final String property = "ecosystem";
        final String description = "The Basis of the YGGDRASH Ecosystem."
                + "It is also an aggregate and a blockchain containing information"
                + "of all Branch Chains.";
        final BigDecimal fee = BigDecimal.valueOf(100);

        JsonObject contractSample = new JsonObject();
        contractSample.addProperty("contractVersion", TestConstants.STEM_CONTRACT.toString());
        contractSample.add("init", new JsonObject());
        contractSample.addProperty("description", "some description");
        contractSample.addProperty("name", "STEM");
        contractSample.addProperty("isSystem", true);

        JsonArray contracts = new JsonArray();
        contracts.add(contractSample);


        return createBranchJson(name, symbol, property, description, contracts, fee, null);
    }

    private static JsonObject createBranchJson(String name,
                                              String symbol,
                                              String property,
                                              String description,
                                              JsonArray contracts,
                                              BigDecimal fee,
                                              String timestamp) {
        JsonObject branch = new JsonObject();
        branch.addProperty("name", name);
        branch.addProperty("symbol", symbol);
        branch.addProperty("property", property);
        branch.addProperty("description", description);
        branch.add("contracts", contracts);
        if (timestamp == null) {
            branch.addProperty("timestamp", "00000166c837f0c9");
        } else {
            branch.addProperty("timestamp", timestamp);
        }

        String consensusString = new StringBuilder()
                .append("{\"consensus\": {\n")
                .append("    \"algorithm\": \"pbft\",\n")
                .append("    \"period\": \"* * * * * *\"\n")
                .append("    \n}")
                .append("  }").toString();

        JsonObject consensus = new Gson().fromJson(consensusString, JsonObject.class);
        branch.add("consensus", consensus);
        branch.addProperty("fee", fee);

        return branch;
    }

    public static JsonObject signBranch(Wallet wallet, JsonObject raw) {
        if (!raw.has("signature")) {

            Sha3Hash hashForSign = new Sha3Hash(SerializationUtil.serializeJson(raw));
            byte[] signature = wallet.sign(hashForSign.getBytes(), true);
            raw.addProperty("signature", Hex.toHexString(signature));
        }
        return raw;
    }

}
