package io.yggdrash.validator;

import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.TestConstants;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBody;
import io.yggdrash.core.blockchain.TransactionHeader;
import io.yggdrash.core.exception.InternalErrorException;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.proto.Proto;
import org.bson.BsonBinary;
import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;
import org.bson.types.Binary;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;

import static org.junit.Assert.assertTrue;

public class BlockSerializePerformanceTest extends TestConstants.PerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(BlockSerializePerformanceTest.class);

    private static Codec<Document> DOCUMENT_CODEC = new DocumentCodec();
    private static long MAX = 10000L;

    private GenesisBlock genesisBlock;

    @Before
    public void setUp() throws Exception {
        this.genesisBlock = new GenesisBlock();
    }

    @Test
    public void generateGenesisBlock() throws IOException {
        this.genesisBlock.generateGenesisBlockFile();

        ClassLoader classLoader = getClass().getClassLoader();

        File genesisFile = new File(classLoader.getResource("./genesis/genesis.json").getFile());
        String genesisString = FileUtil.readFileToString(genesisFile, StandardCharsets.UTF_8);
        Assert.assertNotNull(genesisString);
        log.debug(genesisString);
    }

    @Test
    public void testBlockSize() throws IOException {
        Block block = this.genesisBlock.getGenesisBlock();
        BlockHusk blockHusk = new BlockHusk(genesisBlock.getGenesisBlock());
        Proto.Block blockProto = blockHusk.getProtoBlock();
        JsonObject jsonObject = block.toJsonObject();
        byte[] bsonBytes = convertBlockToBson(block);

        log.info("Block size: {} ", block.toBinary().length);
        log.info("BlockHusk serialize size: {}", blockHusk.getData().length);
        log.info("Proto.Block serialize size: {}", blockProto.toByteArray().length);
        log.info("Json serialize size: {}", jsonObject.toString().length());
        log.info("Bson serialize size: {}", bsonBytes.length);
    }

    private byte[] convertBlockToBson(Block block) throws IOException {
        Document bsonDoc = new Document();
        bsonDoc.append("header", new BsonBinary(convertBlockHeaderToBson(block.getHeader())))
                .append("signature", new BsonBinary(block.getSignature()))
                .append("body", new BsonBinary(convertBlockBodyToBson(block.getBody())));
        return toBinary(bsonDoc);
    }

    private byte[] convertBlockHeaderToBson(BlockHeader blockHeader) {
        Document bsonDoc = new Document();
        bsonDoc.append("chain", new BsonBinary(blockHeader.getChain()))
                .append("version", new BsonBinary(blockHeader.getVersion()))
                .append("type", new BsonBinary(blockHeader.getType()))
                .append("prevBlockHash", new BsonBinary(blockHeader.getPrevBlockHash()))
                .append("index", new BsonInt64(blockHeader.getIndex()))
                .append("timestamp", new BsonTimestamp(blockHeader.getTimestamp()))
                .append("merkleRoot", new BsonBinary(blockHeader.getMerkleRoot()))
                .append("bodyLength", new BsonInt64(blockHeader.getBodyLength()));

        return toBinary(bsonDoc);
    }

    private byte[] convertBlockBodyToBson(BlockBody blockbody) throws IOException {
        BsonDocument bsonDocument = new BsonDocument();
        bsonDocument.append("chain", new BsonBinary(blockbody.toBinary()));

        BasicOutputBuffer buffer;
        BsonBinaryWriter writer;

        buffer = new BasicOutputBuffer();
        writer = new BsonBinaryWriter(buffer);

        BsonDocumentCodec documentCodec = new BsonDocumentCodec();
        documentCodec.encode(writer, bsonDocument, EncoderContext.builder().build());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        buffer.pipe(baos);

        return baos.toByteArray();
    }

    private static byte[] toBinary(final Document document) {
        BasicOutputBuffer outputBuffer = new BasicOutputBuffer();
        BsonBinaryWriter writer = new BsonBinaryWriter(outputBuffer);
        DOCUMENT_CODEC.encode(writer, document, EncoderContext.builder().isEncodingCollectibleDocument(true).build());
        return outputBuffer.toByteArray();
    }

    private static Document toDocument(final byte[] input) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(input);
        outputStream.close();
        BsonBinaryReader bsonReader = new BsonBinaryReader(ByteBuffer.wrap(outputStream.toByteArray()));
        return DOCUMENT_CODEC.decode(bsonReader, DecoderContext.builder().build());
    }


    public byte[] toBinaryBlockHeader(BlockHeader header) {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        try {
            bao.write(header.getChain());
            bao.write(header.getVersion());
            bao.write(header.getType());
            bao.write(header.getPrevBlockHash());
            bao.write(ByteUtil.longToBytes(header.getIndex()));
            bao.write(ByteUtil.longToBytes(header.getTimestamp()));
            bao.write(header.getMerkleRoot());
            bao.write(ByteUtil.longToBytes(header.getBodyLength()));
            bao.close();
            return bao.toByteArray();
        } catch (IOException e) {
            throw new InternalErrorException("toBinary error");
        }
    }

    public byte[] toBinaryTxHeader(TransactionHeader header) {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        try {
            bao.write(header.getChain());
            bao.write(header.getVersion());
            bao.write(header.getType());
            bao.write(ByteUtil.longToBytes(header.getTimestamp()));
            bao.write(header.getBodyHash());
            bao.write(ByteUtil.longToBytes(header.getBodyLength()));
            bao.close();
            return bao.toByteArray();
        } catch (IOException e) {
            throw new InternalErrorException("toBinary error");
        }
    }

    public byte[] toBinaryTx(Transaction tx) {
        TransactionHeader header = tx.getHeader();
        TransactionBody body = tx.getBody();
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        try {
            bao.write(toBinaryTxHeader(header));
            bao.write(tx.getSignature());
            bao.write(body.toBinary());
            bao.close();
            return bao.toByteArray();
        } catch (IOException e) {
            log.warn("Transaction toBinary() IOException");
            throw new NotValidateException();
        }
    }

    public byte[] toBinaryBlockBody(BlockBody body) {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        try {
            for (Transaction tx : body.getBody()) {
                bao.write(toBinaryTx(tx));
            }
            bao.close();
            return bao.toByteArray();
        } catch (IOException e) {
            throw new InternalErrorException("toBinary error");
        }
    }

    public byte[] toBinaryBlock(Block block) {
        BlockHeader header = block.getHeader();
        byte[] signature = block.getSignature();
        BlockBody body = block.getBody();

        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        try {
            bao.write(toBinaryBlockHeader(header));
            bao.write(signature);
            bao.write(toBinaryBlockBody(body));
            bao.close();
            return bao.toByteArray();
        } catch (IOException e) {
            log.warn("Block toBinary() IOException");
            throw new NotValidateException();
        }
    }

    @Test
    public void testBlockToBinary() {
        long startTime;
        long endTime;

        Block block = this.genesisBlock.getGenesisBlock();

        startTime = System.nanoTime();
        for (long l = 0; l < MAX; l++) {
            Block newBlock = new Block(toBinaryBlock(block));
            assertTrue(newBlock.verify());
        }
        endTime = System.nanoTime();

        log.info("testBlockToBinary {} Time: {} ", MAX, endTime - startTime);
    }

    @Test
    public void testBlockHuskToBinary() throws InvalidProtocolBufferException {
        long startTime;
        long endTime;

        BlockHusk blockHusk = new BlockHusk(genesisBlock.getGenesisBlock());

        startTime = System.nanoTime();
        for (long l = 0; l < MAX; l++) {
            Proto.Block newProtoBlock = Proto.Block.parseFrom(blockHusk.getData());
            assertTrue(verifyProto(newProtoBlock));
        }
        endTime = System.nanoTime();

        log.info("testBlockHuskToBinary {} Time: {} ", MAX, endTime - startTime);
    }

    @Test
    public void testBsonToBinary() throws IOException {
        Block block = this.genesisBlock.getGenesisBlock();
        Document bsonDoc = new Document();
        bsonDoc.append("header", new BsonBinary(convertBlockHeaderToBson(block.getHeader())))
                .append("signature", new BsonBinary(block.getSignature()))
                .append("body", new BsonBinary(convertBlockBodyToBson(block.getBody())));

        long startTime = System.nanoTime();
        for (long l = 0; l < MAX; l++) {
            Document newBsonDoc = toDocument(toBinary(bsonDoc));
            assertTrue(verifyBson(newBsonDoc));
        }
        long endTime = System.nanoTime();

        log.info("testBsonToBinary {} Time: {} ", MAX, endTime - startTime);
    }

    private boolean verifyProto(Proto.Block protoBlock) {
        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(protoBlock.getSignature().toByteArray());
        byte[] hashedHeader = HashUtil.sha3(protoBlock.getHeader().toByteArray());

        ECKey ecKeyPub;
        try {
            ecKeyPub = ECKey.signatureToKey(hashedHeader, ecdsaSignature);
        } catch (SignatureException e) {
            throw new InvalidSignatureException(e);
        }

        return ecKeyPub.verify(hashedHeader, ecdsaSignature);
    }

    private boolean verifyBson(Document bsonDoc) {
        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(
                ((Binary) bsonDoc.get("signature")).getData());
        byte[] hashedHeader = HashUtil.sha3(((Binary) bsonDoc.get("header")).getData());

        ECKey ecKeyPub;
        try {
            ecKeyPub = ECKey.signatureToKey(hashedHeader, ecdsaSignature);
        } catch (SignatureException e) {
            throw new InvalidSignatureException(e);
        }

        return ecKeyPub.verify(hashedHeader, ecdsaSignature);
    }

}