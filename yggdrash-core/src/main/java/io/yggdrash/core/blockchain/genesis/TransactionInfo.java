package io.yggdrash.core.blockchain.genesis;

import java.util.List;

public class TransactionInfo {
    public TransactionInfoHeader header;
    public String signature;
    public List<TransactionInfoBody> body;
}