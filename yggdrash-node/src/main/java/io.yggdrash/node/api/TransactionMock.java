package io.yggdrash.node.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class TransactionMock {

    public String retTxMock() {
        JsonObject txObj = new JsonObject();
        JsonObject txData = new JsonObject();

        txObj.addProperty("version", "0");
        txObj.addProperty("type", "00000000000000");
        txObj.addProperty("timestamp", "155810745733540");
        txObj.addProperty("from", "04a0cb0bc45c5889b8136127409de1ae7d3f668e5f29115730362823ed5223aff9b2c22210280af1249e27b08bdeb5c0160af74ec5237292b5ee94bd148c9aabbb");
        txObj.addProperty("dataHash", "ba5f3ea40e95f49bce11942f375ebd3882eb837976eda5c0cb78b9b99ca7b485");
        txObj.addProperty("dataSize", "13");
        txObj.addProperty("signature", "b86e02880e12c575e56c5d15e1f491595219295076721a5bfb6042463d6a2d768331691db0b8de852390305c0f2b218e596e4a59bf54029cf6a8b9afdbb274104");
        txObj.addProperty("transactionHash", "c6b5e583ec18891e9de0e29c3f0358a5c99c474bc3ee78e90c618db72193c0");
        txObj.addProperty("transactionData", txData.toString());

        return txObj.toString();
    }
}


