package io.yggdrash.core.blockchain.genesis;

import java.util.List;

public class BlockInfo {
    public BlockInfoHeader header;
    public String signature;
    public List<TransactionInfo> body;
}
