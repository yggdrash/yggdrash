package io.yggdrash.core.genesis;

import java.util.List;

public class TransactionInfo {
    public TransactionInfoHeader header;
    public String signature;
    public List<GenesisBody> body;
}