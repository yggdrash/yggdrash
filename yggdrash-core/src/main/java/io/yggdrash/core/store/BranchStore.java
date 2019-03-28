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

package io.yggdrash.core.store;

import com.google.common.primitives.Longs;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.vo.PrefixKeyEnum;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.store.BranchStateStore;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchContract;
import io.yggdrash.core.blockchain.BranchId;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BranchStore implements ReadWriterStore<String, String>, BranchStateStore {
    private final DbSource<byte[], byte[]> db;

    // TODO Change to DAO patten
    BranchStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
    }

    @Override
    public void put(String key, String value) {
        db.put(key.getBytes(), value.getBytes());
    }

    @Override
    public String get(String key) {
        return new String(db.get(key.getBytes()));
    }

    private JsonObject getJson(String key) {
        byte[] result = db.get(key.getBytes());
        if (result == null) {
            return null;
        }
        String tempValue = new String(result, StandardCharsets.UTF_8);
        return JsonUtil.parseJsonObject(tempValue);
    }

    private void putJson(String key, JsonObject value) {
        byte[] tempValue = value.toString().getBytes(StandardCharsets.UTF_8);
        db.put(key.getBytes(), tempValue);
    }

    @Override
    public boolean contains(String key) {
        return db.get(key.getBytes()) != null;
    }

    @Override
    public void close() {
        db.close();
    }

    public Long getBestBlock() {
        return reStoreToLong(BlockchainMetaInfo.BEST_BLOCK_INDEX.toString(), -1);
    }

    public void setBestBlock(BlockHusk block) {
        setBestBlockHash(block.getHash());
        setBestBlock(block.getIndex());
    }

    private void setBestBlock(Long index) {
        storeLongValue(BlockchainMetaInfo.BEST_BLOCK_INDEX.toString(), index);
    }

    Sha3Hash getBestBlockHash() {
        byte[] bestBlockHashArray = db.get(BlockchainMetaInfo.BEST_BLOCK.toString().getBytes());
        Sha3Hash bestBlockHash = null;
        if (bestBlockHashArray != null) {
            bestBlockHash = Sha3Hash.createByHashed(bestBlockHashArray);
        }
        return bestBlockHash;
    }

    void setBestBlockHash(Sha3Hash hash) {
        byte[] bestBlockHash = hash.getBytes();
        db.put(BlockchainMetaInfo.BEST_BLOCK.toString().getBytes(), bestBlockHash);
    }

    public Long getLastExecuteBlockIndex() {
        return reStoreToLong(BlockchainMetaInfo.LAST_EXECUTE_BLOCK_INDEX.toString(), -1);
    }

    public Sha3Hash getLastExecuteBlockHash() {
        byte[] lastBlockBytes = db.get(BlockchainMetaInfo.LAST_EXECUTE_BLOCK.toString().getBytes());
        Sha3Hash lastBlockHash = null;
        if (lastBlockBytes != null) {
            lastBlockHash = Sha3Hash.createByHashed(lastBlockBytes);
        }
        return lastBlockHash;
    }

    public  void setLastExecuteBlock(BlockHusk block) {
        storeLongValue(BlockchainMetaInfo.LAST_EXECUTE_BLOCK_INDEX.toString(), block.getIndex());
        byte[] executeBlockHash = block.getHash().getBytes();
        db.put(BlockchainMetaInfo.LAST_EXECUTE_BLOCK.toString().getBytes(), executeBlockHash);
    }

    private Long reStoreToLong(String key, long defaultValue) {
        byte[] longByteArray = db.get(key.getBytes());
        if (longByteArray == null) {
            return defaultValue;
        } else {
            return Longs.fromByteArray(longByteArray);
        }
    }

    private void storeLongValue(String key, long value) {
        byte[] longValue = Longs.toByteArray(value);
        db.put(key.getBytes(), longValue);
    }


    public void setBranch(Branch branch) {
        // if Exist Branch Information Did not save
        // Save Branch
        JsonObject json = branch.getJson();
        if (db.get(BlockchainMetaInfo.BRANCH.toString().getBytes()) == null) {
            db.put(BlockchainMetaInfo.BRANCH.toString().getBytes(), json.toString().getBytes());
            db.put(BlockchainMetaInfo.BRANCH_ID.toString().getBytes(), branch.getBranchId().getBytes());
        }
    }

    public Branch getBranch() {
        // load Branch
        byte[] jsonByteArray = db.get(BlockchainMetaInfo.BRANCH.toString().getBytes());
        String jsonString = new String(jsonByteArray);
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(jsonString).getAsJsonObject();

        return Branch.of(json);
    }

    public BranchId getBranchId() {
        return new BranchId(getBranchIdHash());
    }

    public Sha3Hash getBranchIdHash() {
        byte[] branchIdBytes = db.get(BlockchainMetaInfo.BRANCH_ID.toString().getBytes());
        return new Sha3Hash(branchIdBytes, true);
    }

    // TODO UPDATE Branch - Version History

    // Set Genesis Block
    public boolean setGenesisBlockHash(Sha3Hash genesisBlockHash) {
        if (db.get(BlockchainMetaInfo.GENESIS_BLOCK.toString().getBytes()) == null) {
            db.put(BlockchainMetaInfo.GENESIS_BLOCK.toString().getBytes(), genesisBlockHash.getBytes());
            return true;
        }
        return false;
    }

    // Get Genesis Block
    public Sha3Hash getGenesisBlockHash() {
        byte[] genesisBlockHash = db.get(BlockchainMetaInfo.GENESIS_BLOCK.toString().getBytes());
        if (genesisBlockHash == null) {
            return null;
        }
        return new Sha3Hash(genesisBlockHash, true);
    }

    // Set Validator
    public void setValidators(ValidatorSet validatorSet) {
        JsonObject jsonValidator = JsonUtil.parseJsonObject(JsonUtil.convertObjToString(validatorSet));
        putJson(PrefixKeyEnum.VALIDATORS.toValue(), jsonValidator);
    }

    // Get Validator
    public ValidatorSet getValidators() {
        ValidatorSet validatorSet = null;
        JsonObject jsonValidatorSet = getJson(PrefixKeyEnum.VALIDATORS.toValue());
        if (jsonValidatorSet != null) {
            validatorSet = JsonUtil.generateJsonToClass(jsonValidatorSet.toString(), ValidatorSet.class);
        }
        return validatorSet;
    }

    // Set Contracts
    // Save Contracts initial values
    public void setBranchContracts(List<BranchContract> contracts) {
        JsonArray array = new JsonArray();
        contracts.stream().forEach(c -> array.add(c.getJson()));
        byte[] contractBytes = array.toString().getBytes();
        db.put(BlockchainMetaInfo.BRANCH_CONTRACTS.toString().getBytes(), contractBytes);
    }

    // Get Contracts
    // Load Contracts initial values
    public List<BranchContract> getBranchContacts() {
        List<BranchContract> contracts = new ArrayList<>();
        byte[] contractBytes = db.get(BlockchainMetaInfo.BRANCH_CONTRACTS.toString().getBytes());
        if (contractBytes == null) {
            return new ArrayList<>();
        }
        JsonParser parser = new JsonParser();
        JsonArray json = parser.parse(new String(contractBytes)).getAsJsonArray();

        for (int i = 0; i < json.size(); i++) {
            if (json.get(i).isJsonObject()) {
                JsonObject branchContract = json.get(i).getAsJsonObject();
                contracts.add(BranchContract.of(branchContract));
            }
        }
        return contracts;
    }

    public enum  BlockchainMetaInfo {
        BEST_BLOCK,
        BEST_BLOCK_INDEX,
        LAST_EXECUTE_BLOCK,
        LAST_EXECUTE_BLOCK_INDEX,
        BRANCH,
        BRANCH_ID,
        GENESIS_BLOCK,
        VALIDATORS,
        BRANCH_CONTRACTS
    }
}
