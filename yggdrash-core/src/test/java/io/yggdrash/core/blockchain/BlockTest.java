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

import com.google.gson.JsonObject;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.common.util.VerifierUtils;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.wallet.Wallet;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockTest {

    private static final Logger log = LoggerFactory.getLogger(BlockTest.class);

    private final byte[] chain = Constants.EMPTY_BRANCH;
    private final byte[] version = Constants.EMPTY_BYTE8;
    private final byte[] type = Constants.EMPTY_BYTE8;
    private final byte[] prevBlockHash = Constants.EMPTY_HASH;

    private final Wallet wallet = TestConstants.wallet();
    private Block block1;

    @Before
    public void init() {
        JsonObject jsonParams = new JsonObject();
        jsonParams.addProperty("address", "5db10750e8caff27f906b41c71b3471057dd2000");
        jsonParams.addProperty("amount", "10000000");

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("method", "transfer");
        jsonObject.add("params", jsonParams);

        TransactionBody txBody = new TransactionBody(jsonObject);

        long timestamp = TimeUtils.time();
        TransactionHeader txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);

        Transaction tx1 = new TransactionImpl(txHeader, wallet, txBody);
        Transaction tx2 = new TransactionImpl(tx1.toBinary());

        List<Transaction> txs1 = new ArrayList<>();
        txs1.add(tx1);
        txs1.add(tx2);

        log.debug("txs={}", txs1.toString());

        BlockBody blockBody1 = new BlockBody(txs1);

        timestamp = TimeUtils.time();
        long index = 0;
        BlockHeader blockHeader = new BlockHeader(
                chain, version, type, prevBlockHash, index, timestamp,
                blockBody1.getMerkleRoot(), blockBody1.getStateRoot(), blockBody1.getLength());

        log.debug(blockHeader.toString());

        BlockSignature blockSig = new BlockSignature(wallet, blockHeader.getHashForSigning());
        block1 = new BlockImpl(blockHeader, blockSig.getSignature(), blockBody1);
    }

    @Test
    public void testEmptyBlockVerify() {
        BlockBody blockBody = new BlockBody(Collections.emptyList());
        BlockHeader blockHeader = new BlockHeader(
                chain, version, type, prevBlockHash, 0, TimeUtils.time(),
                blockBody.getMerkleRoot(), blockBody.getStateRoot(), blockBody.getLength());
        BlockSignature blockSig = new BlockSignature(wallet, blockHeader.getHashForSigning());
        Block emptyBlock = new BlockImpl(blockHeader, blockSig.getSignature(), blockBody);

        assertThat(VerifierUtils.verify(emptyBlock)).isTrue();
    }

    @Test
    public void shouldBeLoadedBranchJsonFile() throws IOException {
        ClassLoader loader = BlockTest.class.getClassLoader();
        InputStream is = loader.getResourceAsStream("branch-yggdrash.json");
        GenesisBlock genesisBlock = BlockChainTestUtils.generateGenesisBlockByInputStream(is);

        assertThat(genesisBlock.getBlock()).isNotNull();
        assertThat(genesisBlock.getBlock().getIndex()).isEqualTo(0);
    }

    @Test
    public void testBlockConstructor() {
        BlockHeader blockHeader2 = new BlockHeader(block1.getHeader().toBinary());
        BlockBody blockBody2 = new BlockBody(block1.getBody().toBinary());
        BlockSignature blockSig2 = new BlockSignature(wallet, blockHeader2.getHashForSigning());
        Block block2 = new BlockImpl(blockHeader2, blockSig2.getSignature(), blockBody2);
        assertThat(VerifierUtils.verify(block2)).isTrue();
        assertThat(block1.toJsonObject()).isEqualTo(block2.toJsonObject());

        Block block3 = new BlockImpl(blockHeader2, wallet, block2.getBody());
        assertThat(VerifierUtils.verify(block3)).isTrue();
        assertThat(block1.toJsonObject()).isEqualTo(block3.toJsonObject());

        Block block4 = new BlockImpl(block1.toJsonObject());
        assertThat(VerifierUtils.verify(block4)).isTrue();
        assertThat(block1.toJsonObject().toString()).isEqualTo(block4.toJsonObject().toString());

        Block block5 = new BlockImpl(block1.getProtoBlock().toByteArray());
        assertThat(VerifierUtils.verify(block5)).isTrue();
        assertThat(block1.toJsonObject().toString()).isEqualTo(block5.toJsonObject().toString());
    }

    @Test
    public void testBlockClone() {
        Block block2 = new BlockImpl(block1.getProtoBlock().toByteArray());
        log.debug("block2={}", block2.toJsonObject());

        assertThat(block1.getHash()).isEqualTo(block2.getHash());
        assertThat(block1.toJsonObject().toString()).isEqualTo(block2.toJsonObject().toString());
        assertThat(block1.getSignature()).isEqualTo(block2.getSignature());
    }

    @Test
    public void testBlockKey() {
        Block block2 = new BlockImpl(block1.getProtoBlock().toByteArray());
        log.debug("block2 pubKey={}", Hex.toHexString(block2.getPubKey()));

        assertThat(block1.getPubKey()).isEqualTo(block2.getPubKey());
        assertThat(block1.getPubKey()).isEqualTo(wallet.getPubicKey());

        log.debug("block1 author address={}", block1.getAddress());
        log.debug("block2 author address={}", block2.getAddress());
        log.debug("wallet address={}", wallet.getHexAddress());
        assertThat(block1.getAddress()).isEqualTo(block2.getAddress());
        assertThat(block1.getAddress().toString()).isEqualTo(wallet.getHexAddress());
        assertThat(VerifierUtils.verify(block1)).isTrue();
        assertThat(VerifierUtils.verify(block2)).isTrue();
    }
}
