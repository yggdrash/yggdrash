package io.yggdrash.node.api;

import com.google.gson.JsonObject;

public class TransactionReceiptMock {

    public String retTxReceiptMock() {
        JsonObject txReceiptObj = new JsonObject();

        JsonObject txLog = new JsonObject();

        txReceiptObj.addProperty("id", 1);
        txReceiptObj.addProperty("jsonrpc", "2.0");
        txReceiptObj.addProperty("transactionHash", "0xb903239f8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238");
        txReceiptObj.addProperty("transactionIndex", "0x1");
        txReceiptObj.addProperty("blockHash", "0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b");
        txReceiptObj.addProperty("cumulativeGasUsed", "0x33bc");
        txReceiptObj.addProperty("gasUsed", "0x4dc");
        txReceiptObj.addProperty("contractAddress", "0xb60e8dd61c5d32be8058bb8eb970870f07233155");
        txReceiptObj.addProperty("logs", txLog.toString());
        txReceiptObj.addProperty("logsBloom", "0x00...0");
        txReceiptObj.addProperty("status", "0x01");

        return txReceiptObj.toString();
    }
}

