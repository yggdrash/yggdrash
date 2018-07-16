package io.yggdrash.core.format;

import io.yggdrash.core.TransactionHeader;

import java.io.IOException;

public interface Transaction {
    public String getHashString() throws IOException;

    public byte[] getHash() throws IOException;

    public String getData();

    public TransactionHeader getHeader();

    public String toString();
}

