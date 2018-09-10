package io.yggdrash.core.genesis;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class TransactionInfo {
    public TransactionInfoHeader header;
    public String signature;
    public List<GenesisParam> body;

    public TransactionInfo() {}
}