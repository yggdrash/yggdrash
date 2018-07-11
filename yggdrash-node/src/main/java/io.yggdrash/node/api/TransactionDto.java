package io.yggdrash.node.api;

import com.google.gson.JsonObject;
import io.yggdrash.core.Account;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.format.TransactionFormat;

import java.io.IOException;
import java.security.SignatureException;

public class TransactionDto {
    private String from;
    private String txHash;
    private String data;

    public static Transaction of(TransactionDto transactionDto) throws IOException {
        // TODO Account from 에서 가져와서 실제 Account로 변환합니다.
        Account account = new Account();
        JsonObject jsonData = new JsonObject();
        jsonData.addProperty("data", transactionDto.getData());
        return new Transaction(account, jsonData);
    }

    public static TransactionDto createBy(TransactionFormat tx)
            throws IOException, SignatureException {
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setFrom(tx.getHeader().getAddressToString());
        transactionDto.setData(tx.getData());
        transactionDto.setTxHash(tx.getHashString());
        return transactionDto;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }
}
