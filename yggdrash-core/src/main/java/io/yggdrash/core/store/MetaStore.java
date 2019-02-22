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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BlockchainMetaInfo;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.store.datasource.DbSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class MetaStore implements Store<String, String> {
    private final DbSource<byte[], byte[]> db;

    MetaStore(DbSource<byte[], byte[]> dbSource) {
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

    public void setBestBlock(Long index) {
        storeLongValue(BlockchainMetaInfo.BEST_BLOCK_INDEX.toString(), index);
    }

    public Sha3Hash getBestBlockHash() {
        byte[] bestBlockHashArray = db.get(BlockchainMetaInfo.BEST_BLOCK.toString().getBytes());
        Sha3Hash bestBlockHash = null;
        if (bestBlockHashArray != null) {
            bestBlockHash = Sha3Hash.createByHashed(bestBlockHashArray);
        }
        return bestBlockHash;
    }

    public void setBestBlockHash(Sha3Hash hash) {
        byte[] bestBlockHash = hash.getBytes();
        db.put(BlockchainMetaInfo.BEST_BLOCK.toString().getBytes(), bestBlockHash);
    }

    public void setBestBlock(BlockHusk block) {
        setBestBlockHash(block.getHash());
        setBestBlock(block.getIndex());
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

    // TODO UPDATE Branch - Version History

    // Set Genesis Block
    public void setGenesisBlock(GenesisBlock genesisBlock) {
        if (db.get(BlockchainMetaInfo.GENESIS_BLOCK.toString().getBytes()) == null) {
            Sha3Hash genesisBlockHash = genesisBlock.getBlock().getHash();
            db.put(BlockchainMetaInfo.GENESIS_BLOCK.toString().getBytes(), genesisBlockHash.getBytes());
        }
    }

    // Get Genesis Block
    public Sha3Hash getGenesisBlock() {
        byte[] genesisBlockHash = db.get(BlockchainMetaInfo.GENESIS_BLOCK.toString().getBytes());
        return new Sha3Hash(genesisBlockHash);
    }

    // Set Validator
    public void setValidators(Set<String> validators) {
        // TODO Set Validators
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        for (String element : validators) {
            try {
                out.writeUTF(element);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        byte[] valiatorsByteArray = baos.toByteArray();
        db.put(BlockchainMetaInfo.VALIDATORS.toString().getBytes(), valiatorsByteArray);
    }

    // TODO Get Validator
    public Set<String> getValidators() throws IOException {
        byte[] valiatorsByteArray = db.get(BlockchainMetaInfo.VALIDATORS.toString().getBytes());
        ByteArrayInputStream bais = new ByteArrayInputStream(valiatorsByteArray);
        DataInputStream in = new DataInputStream(bais);
        Set<String> validatorSet = new HashSet<>();
        while (in.available() > 0) {
            validatorSet.add(in.readUTF());
        }
        return validatorSet;
    }

    // Add validator
    public boolean addValidator(String validator) {
        Set<String> validators = null;
        try {
            validators = getValidators();
            validators.add(validator);
            setValidators(validators);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;

    }

    // Remove Validator
    public boolean removeValidator(String validator) {
        Set<String> validators = null;
        try {
            validators = getValidators();
            validators.remove(validator);
            setValidators(validators);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // TODO Set Contracts
    // Save Contracts initial values

    // TODO Get Contracts
    // Load Contracts initial values



    // TODO Update Contract

    // TODO Add Contract

}
