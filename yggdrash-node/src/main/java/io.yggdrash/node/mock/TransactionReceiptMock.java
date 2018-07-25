package io.yggdrash.node.mock;

import com.google.gson.JsonObject;

import java.util.ArrayList;

public class TransactionReceiptMock {

    public String transactionHash =
            "0xb903239f8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238";
    public int transactionIndex = 1;
    public String blockHash =
            "0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b";
    public int yeedUsed = 30000;
    public String branchAddress =
            "0xb60e8dd61c5d32be8058bb8eb970870f07233155";
    public ArrayList<String> txLog = new ArrayList<>();
    public int status = 1;

    public TransactionReceiptMock() {

    }
}

