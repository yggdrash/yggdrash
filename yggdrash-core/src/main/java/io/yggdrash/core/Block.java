package io.yggdrash.core;

import java.io.IOException;
import java.io.Serializable;

import io.yggdrash.proto.BlockChainProto;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Block implements Cloneable, Serializable {

  private static final Logger log = LoggerFactory.getLogger(Block.class);

  private final BlockHeader header;
  private final BlockBody data;

  public Block(BlockHeader header, BlockBody data) {
    this.header = header;
    this.data = data;
  }


  /**
   * Instantiates a new Block.
   *
   * @param author the author
   * @param prevBlock the prev block
   * @param blockBody the block body
   */
  public Block(Account author, Block prevBlock, BlockBody blockBody) throws IOException {
    this.header = new BlockHeader.Builder()
      .prevBlock(prevBlock)
      .blockBody(blockBody)
      .build(author);

    this.data = blockBody;
  }

  public String getBlockHash() throws IOException {
    return Hex.encodeHexString(header.getBlockHash());
  }

  public String getPrevBlockHash() {
    return header.getPrevBlockHash() == null ? "" :
      Hex.encodeHexString(header.getPrevBlockHash());
  }

  byte[] getBlockByteHash() throws IOException {
    return header.getBlockHash();
  }

  public long getIndex() {
    return header.getIndex();
  }

  public long nextIndex() {
    return header.getIndex() + 1;
  }

  public long getTimestamp() {
    return header.getTimestamp();
  }

  public BlockBody getData() {
    return data;
  }

  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  public static Block valueOf(BlockChainProto.Block protoBlock) {
    BlockHeader header = BlockHeader.valueOf(protoBlock.getHeader());
    BlockBody data = BlockBody.valueOf(protoBlock.getData());
    return new Block(header, data);
  }

  public static BlockChainProto.Block of(Block block) {
    BlockHeader header = block.header;
    BlockBody data = block.data;

    BlockChainProto.BlockBody.Builder bodyBuilder = BlockChainProto.BlockBody.newBuilder();
    for (Transaction tx : data.getTransactionList()) {
      bodyBuilder.addTrasactions(Transaction.of(tx));
    }

    return BlockChainProto.Block.newBuilder()
            .setHeader(BlockHeader.of(header)).setData(bodyBuilder).build();
  }

  @Override
  public String toString() {
    return "Block{"
      + "header=" + header
      + ", data=" + data
      + '}';
  }
}
