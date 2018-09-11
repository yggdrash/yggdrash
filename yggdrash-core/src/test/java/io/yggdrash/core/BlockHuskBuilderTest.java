package io.yggdrash.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class BlockHuskBuilderTest {
    private static final Logger log = LoggerFactory.getLogger(BlockHuskBuilderTest.class);

    Wallet wallet;
    BlockHusk genesis;
    byte[] type = ByteBuffer.allocate(4).putInt(BlockHuskBuilder.DEFAULT_TYPE).array();
    byte[] version = ByteBuffer.allocate(4).putInt(BlockHuskBuilder.DEFAULT_VERSION).array();

    /*
    @Before
    public void setUp() throws Exception {
        this.wallet = new Wallet();
        this.genesis = TestUtils.createGenesisBlockHusk();

        Proto.Block.Header.Raw raw = genesis.getInstance().getHeader().getRawData();

        assertArrayEquals(type, raw.getType().toByteArray());
        assertArrayEquals(version, raw.getVersion().toByteArray());
    }

    @Test
    public void buildBlockWithPrev() {
        TransactionHusk newTx = TestUtils.createTxHusk(wallet);
        BlockHusk block1 =
                BlockHuskBuilder.buildUnSigned(wallet, Collections.singletonList(newTx), genesis);

        assertArrayEquals(genesis.getHash().getBytes(), block1.getPrevHash().getBytes());
        BlockHusk block2 = BlockHuskBuilder.buildSigned(
                wallet, Collections.singletonList(newTx), block1);

        assertArrayEquals(block2.getAddress().getBytes(), wallet.getAddress());
    }

    @Test
    public void buildEmptyGenesisBlock() {
        Proto.Block.Header.Raw raw = Proto.Block.Header.Raw.newBuilder()
                .setType(ByteString.copyFrom(type))
                .setVersion(ByteString.copyFrom(version))
                .setPrevBlockHash(ByteString.copyFrom(BlockHuskBuilder.EMPTY_BYTE))
                .setIndex(0)
                .build();

        BlockHusk genesisBlock = BlockHuskBuilder.buildUnSigned(wallet, raw,
                Collections.emptyList()).sign(wallet);

        Proto.Block.Header.Raw newRaw = genesisBlock.getInstance().getHeader().getRawData();
        assertArrayEquals(wallet.getAddress(), newRaw.getAuthor().toByteArray());
        log.debug(genesisBlock.toJsonObject().toString());
    }

    @Test
    public void buildYeedGenesisBlock() {
        TransactionHusk signedTx = getYeedGenesisTx().sign(wallet);

        Proto.Block.Header.Raw raw = Proto.Block.Header.Raw.newBuilder()
                .setType(ByteString.copyFrom(type))
                .setVersion(ByteString.copyFrom(version))
                .setPrevBlockHash(ByteString.copyFrom(BlockHuskBuilder.EMPTY_BYTE))
                .setIndex(0)
                .build();

        BlockHusk genesisBlock = BlockHuskBuilder.buildUnSigned(wallet, raw,
                Collections.singletonList(signedTx)).sign(wallet);

        Proto.Block.Header.Raw newRaw = genesisBlock.getInstance().getHeader().getRawData();
        assertEquals(signedTx.getData().length, newRaw.getDataSize());
        assertEquals(32, newRaw.getMerkleRoot().toByteArray().length);
        assertArrayEquals(wallet.getAddress(), newRaw.getAuthor().toByteArray());
        log.debug(genesisBlock.toJsonObject().toString());
    }

    private TransactionHusk getYeedGenesisTx() {
        JsonArray params = new JsonArray();
        JsonObject param1 = new JsonObject();
        param1.addProperty("frontier", "f1988f13fb5006c9e02a72133ce18a269f442eb4");
        param1.addProperty("balance", 1000000000);
        params.add(param1);
        JsonObject param2 = new JsonObject();
        param2.addProperty("frontier", "1389397cfbdb31a2c7d9640ad293e099ba585c9b");
        param2.addProperty("balance", 1);
        params.add(param2);

        JsonObject data = new JsonObject();
        data.addProperty("method", "GENESIS");
        data.addProperty("branchId", "1234");
        data.addProperty("branchName", "YEED");
        data.add("params", params);
        return TestUtils.createTxHuskByJson(data);
    }
    */
}