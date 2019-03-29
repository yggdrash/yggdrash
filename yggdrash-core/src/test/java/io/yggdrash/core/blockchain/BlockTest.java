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

package io.yggdrash.core.blockchain;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.TestConstants;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.wallet.Wallet;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockTest {

    private static final Logger log = LoggerFactory.getLogger(BlockTest.class);

    private final byte[] chain = new byte[20];
    private final byte[] version = new byte[8];
    private final byte[] type = new byte[8];
    private final byte[] prevBlockHash = new byte[32];

    private final Wallet wallet = TestConstants.wallet();
    private Block block1;

    @Before
    public void init() {
        JsonObject jsonParams1 = new JsonObject();
        jsonParams1.addProperty("address", "5db10750e8caff27f906b41c71b3471057dd2000");
        jsonParams1.addProperty("amount", "10000000");

        JsonObject jsonObject1 = new JsonObject();
        jsonObject1.addProperty("method", "transfer");
        jsonObject1.add("params", jsonParams1);

        JsonObject jsonParams2 = new JsonObject();
        jsonParams2.addProperty("address", "5db10750e8caff27f906b41c71b3471057dd2001");
        jsonParams2.addProperty("amount", "5000000");

        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("method", "transfer");
        jsonObject2.add("params", jsonParams2);

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(jsonObject1);
        jsonArray.add(jsonObject2);

        TransactionBody txBody = new TransactionBody(jsonArray);

        long timestamp = TimeUtils.time();
        TransactionHeader txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);
        TransactionSignature txSig = new TransactionSignature(wallet, txHeader.getHashForSigning());

        Transaction tx1 = new Transaction(txHeader, txSig.getSignature(), txBody);
        Transaction tx2 = new Transaction(tx1.toBinary());

        List<Transaction> txs1 = new ArrayList<>();
        txs1.add(tx1);
        txs1.add(tx2);

        log.debug("txs=" + txs1.toString());

        BlockBody blockBody1 = new BlockBody(txs1);

        timestamp = TimeUtils.time();
        long index = 0;
        BlockHeader blockHeader = new BlockHeader(
                chain, version, type, prevBlockHash, index, timestamp,
                blockBody1.getMerkleRoot(), blockBody1.length());

        log.debug(blockHeader.toString());

        BlockSignature blockSig = new BlockSignature(wallet, blockHeader.getHashForSigning());
        block1 = new Block(blockHeader, blockSig.getSignature(), blockBody1);
    }

    @Test
    public void testEmptyBlockVerify() {
        BlockBody blockBody = new BlockBody(Collections.emptyList());
        BlockHeader blockHeader = new BlockHeader(
                chain, version, type, prevBlockHash, 0, TimeUtils.time(),
                blockBody.getMerkleRoot(), blockBody.length());
        BlockSignature blockSig = new BlockSignature(wallet, blockHeader.getHashForSigning());
        Block emptyBlock = new Block(blockHeader, blockSig.getSignature(), blockBody);
        assertThat(emptyBlock.verify()).isTrue();
    }

    @Test
    public void shouldBeLoadedBranchJsonFile() throws IOException {
        ClassLoader loader = BlockTest.class.getClassLoader();
        InputStream is = loader.getResourceAsStream("branch-sample.json");
        GenesisBlock genesisBlock = GenesisBlock.of(is);

        assertThat(genesisBlock.getBlock()).isNotNull();
        assertThat(genesisBlock.getBlock().getIndex()).isEqualTo(0);
    }

    @Test
    public void testBlockConstructor() {
        BlockHeader blockHeader2 = new BlockHeader(block1.getHeader().toBinary());
        BlockBody blockBody2 = new BlockBody(block1.getBody().toBinary());
        BlockSignature blockSig2 = new BlockSignature(wallet, blockHeader2.getHashForSigning());
        Block block2 = new Block(blockHeader2, blockSig2.getSignature(), blockBody2);

        assertThat(block2.verify()).isTrue();
        assertThat(block1.toJsonObject()).isEqualTo(block2.toJsonObject());

        Block block3 = new Block(blockHeader2, wallet, block2.getBody());

        assertThat(block3.verify()).isTrue();
        assertThat(block1.toJsonObject()).isEqualTo(block3.toJsonObject());

        Block block4 = new Block(block1.toJsonObject());
        assertThat(block4.verify()).isTrue();
        assertThat(block1.toJsonObject().toString()).isEqualTo(block4.toJsonObject().toString());

        Block block5 = new Block(block1.toBinary());
        assertThat(block5.verify()).isTrue();
        assertThat(block1.toJsonObject().toString()).isEqualTo(block5.toJsonObject().toString());
    }

    @Test
    public void testBlockClone() {
        Block block2 = new Block(block1.toBinary());
        log.debug("block2=" + block2.toJsonObject());

        assertThat(block1.getHashHex()).isEqualTo(block2.getHashHex());
        assertThat(block1.toJsonObject().toString()).isEqualTo(block2.toJsonObject().toString());
        assertThat(block1.getSignature()).isEqualTo(block2.getSignature());
    }

    @Test
    public void testBlockKey() {
        Block block2 = new Block(block1.toBinary());
        log.debug("block2 pubKey=" + block2.getPubKeyHex());

        assertThat(block1.getPubKeyHex()).isEqualTo(block2.getPubKeyHex());
        assertThat(block1.getPubKey()).isEqualTo(block2.getPubKey());
        assertThat(block1.getPubKey()).isEqualTo(wallet.getPubicKey());

        log.debug("block1 author address=" + block1.getAddressHex());
        log.debug("block2 author address=" + block2.getAddressHex());
        log.debug("wallet address=" + wallet.getHexAddress());
        assertThat(block1.getAddressHex()).isEqualTo(block2.getAddressHex());
        assertThat(block1.getAddressHex()).isEqualTo(wallet.getHexAddress());
        assertThat(block1.verify()).isTrue();
        assertThat(block2.verify()).isTrue();
    }
}
