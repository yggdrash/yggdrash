package io.yggdrash.core.blockchain;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.proto.Proto;
import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;
import org.bson.BasicBSONEncoder;
import org.bson.BasicBSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

public class BlockHeaderSerializeTest extends TestConstants.PerformanceTest {
    private static final Logger log = LoggerFactory.getLogger(BlockHeaderSerializeTest.class);
    private static final int MAX_TEST_NUM = 1000000;
    private Block genesis;

    @Before
    public void setUp() {
        this.genesis = BlockChainTestUtils.genesisBlock().getCoreBlock();
    }

    @Test(timeout = 1000L)
    public void testCoreBlock() {
        BlockHeader blockHeader = genesis.getHeader();

        byte[] binary = blockHeader.toBinary();
        log.info("CoreBlock toBinary().length={}, toString()={}", binary.length, blockHeader);
        Assert.assertEquals(blockHeader, new BlockHeader(binary));

        // to binary test
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < MAX_TEST_NUM; i++) {
            blockHeader.toBinary();
        }
        long totalMilllis = System.currentTimeMillis() - startTime;
        log.info("CoreBlock toBinary() count={}, time={}", MAX_TEST_NUM, totalMilllis);

        // to object test
        startTime = System.currentTimeMillis();
        for (int i = 0; i < MAX_TEST_NUM; i++) {
            new BlockHeader(binary);
        }
        totalMilllis = System.currentTimeMillis() - startTime;
        log.info("CoreBlock toObject() count={}, time={}", MAX_TEST_NUM, totalMilllis);
    }

    @Test(timeout = 1000L)
    public void testProtoBlock() {
        Proto.Block.Header blockHeader = genesis.toProtoBlock().getHeader();

        byte[] binary = blockHeader.toByteArray();
        log.info("ProtoBlock toBinary().length={}, toString()={}", binary.length, toJson(blockHeader));
        Assert.assertEquals(blockHeader, parseFrom(binary));

        // to binary test
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < MAX_TEST_NUM; i++) {
            blockHeader.toByteArray();
        }
        long totalMilllis = System.currentTimeMillis() - startTime;
        log.info("ProtoBlock toBinary() count={}, time={}", MAX_TEST_NUM, totalMilllis);

        // to object test
        startTime = System.currentTimeMillis();
        for (int i = 0; i < MAX_TEST_NUM; i++) {
            parseFrom(binary);
        }
        totalMilllis = System.currentTimeMillis() - startTime;
        log.info("ProtoBlock toObject() count={}, time={}", MAX_TEST_NUM, totalMilllis);
    }

    @Test
    public void testBsonBlock() {
        BSONObject blockHeader = toBlockHeaderBson(genesis.getHeader());

        byte[] binary = new BasicBSONEncoder().encode(blockHeader);
        log.info("BsonBlock toBinary().length={}, toString()={}", binary.length, blockHeader);
        Assert.assertEquals(blockHeader, new BasicBSONDecoder().readObject(binary));

        // to binary test
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < MAX_TEST_NUM; i++) {
            new BasicBSONEncoder().encode(blockHeader);
        }
        long totalMilllis = System.currentTimeMillis() - startTime;
        log.info("BsonBlock toBinary() count={}, time={}", MAX_TEST_NUM, totalMilllis);

        // to object test
        startTime = System.currentTimeMillis();
        for (int i = 0; i < MAX_TEST_NUM; i++) {
            new BasicBSONDecoder().readObject(binary);
        }
        totalMilllis = System.currentTimeMillis() - startTime;
        log.info("BsonBlock toObject() count={}, time={}", MAX_TEST_NUM, totalMilllis);
    }

    private Proto.Block.Header parseFrom(byte[] data) {
        try {
            return Proto.Block.Header.parseFrom(data);
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
    }

    private static BSONObject toBlockHeaderBson(BlockHeader blockHeader) {
        BSONObject bsonObject = new BasicBSONObject();

        bsonObject.put("chain", blockHeader.getChain());
        bsonObject.put("version", blockHeader.getVersion());
        bsonObject.put("type", blockHeader.getType());
        bsonObject.put("prevBlockHash", blockHeader.getPrevBlockHash());
        bsonObject.put("index", ByteUtil.longToBytes(blockHeader.getIndex()));
        bsonObject.put("timestamp", ByteUtil.longToBytes(blockHeader.getTimestamp()));
        bsonObject.put("merkleRoot", blockHeader.getMerkleRoot());
        bsonObject.put("bodyLength", ByteUtil.longToBytes(blockHeader.getBodyLength()));
        return bsonObject;
    }

    private static BSONObject toBlockHeaderHexBson(BlockHeader blockHeader) {
        BSONObject bsonObject = new BasicBSONObject();

        bsonObject.put("chain", Hex.toHexString(blockHeader.getChain()));
        bsonObject.put("version", Hex.toHexString(blockHeader.getVersion()));
        bsonObject.put("type", Hex.toHexString(blockHeader.getType()));
        bsonObject.put("prevBlockHash", Hex.toHexString(blockHeader.getPrevBlockHash()));
        bsonObject.put("index", Hex.toHexString(ByteUtil.longToBytes(blockHeader.getIndex())));
        bsonObject.put("timestamp", Hex.toHexString(ByteUtil.longToBytes(blockHeader.getTimestamp())));
        bsonObject.put("merkleRoot", Hex.toHexString(blockHeader.getMerkleRoot()));
        bsonObject.put("bodyLength", Hex.toHexString(ByteUtil.longToBytes(blockHeader.getBodyLength())));
        return bsonObject;
    }

    private JsonElement toJson(Proto.Block.Header blockHeader) {
        try {
            return new JsonParser().parse(JsonFormat.printer().print(blockHeader));
        } catch (InvalidProtocolBufferException e) {
            throw new FailedOperationException(e);
        }
    }
}
