package io.yggdrash.core;

import io.yggdrash.contract.CoinContract;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.lang.reflect.Method;


public class Runtime {

    public void execute(CoinContract coinContract, TransactionHusk tx) throws Exception {
        String data = tx.getBody();
        JSONParser jsonParser = new JSONParser();
        JSONObject txBody = (JSONObject) jsonParser.parse(data);
        String operator = txBody.get("operator").toString().toLowerCase();
        String from = tx.getAddress().toString();
        String to = txBody.get("to").toString().toUpperCase();
        String amount = txBody.get("amount").toString();

        Method contractMethod =
                CoinContract.class.getMethod(operator, String.class, String.class, String.class);
        TransactionReceipt res =
                (TransactionReceipt) contractMethod.invoke(coinContract, from, to, amount);
    }
}
