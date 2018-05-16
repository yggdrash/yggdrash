package io.yggdrash.node;

import io.yggdrash.core.Transaction;

public class TxDto {
    private String from;
    private String txHash;
    private String data;

    public static Transaction of(TxDto txDto) {
        return new Transaction(txDto.getData());
    }

    public static TxDto createBy(Transaction tx) {
        TxDto txDto = new TxDto();
        txDto.setFrom(tx.getFrom());
        txDto.setData(tx.getData());
        txDto.setTxHash(tx.getHashString());
        return txDto;
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
