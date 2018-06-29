package io.yggdrash.node.api;

import io.yggdrash.core.Account;
import io.yggdrash.core.Transaction;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.TimeUtils;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@Import(ApplicationConfig.class)
public class TransactionAPITest {

    @Test
    public void test() throws Exception {

        TransactionMock txMock = new TransactionMock();
        String txObj = txMock.retTxMock();

        TransactionReceiptMock txReceiptMock = new TransactionReceiptMock();
        String txReceiptObj = txReceiptMock.retTxReceiptMock();

        TransactionAPIImpl txapi = new TransactionAPIImpl();

        String address = "0x407d73d8a49eeb85d32cf465507dd71d507100c1";
        String tag = "latest";
        String hashOfBlock = "0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf";
        String hashOfTx = "0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf";
        int blockNumber = 1;
        int txIndexPosition = 1;

        assertThat(1).isEqualTo(txapi.getTransactionCount(address, tag));
        assertThat(2).isEqualTo(txapi.getTransactionCount(address, blockNumber));

        assertThat(3).isEqualTo(txapi.getBlockTransactionCountByHash(hashOfBlock));
        assertThat(4).isEqualTo(txapi.getBlockTransactionCountByNumber(blockNumber));
        assertThat(5).isEqualTo(txapi.getBlockTransactionCountByNumber(tag));

        assertThat(txObj).isEqualTo(txapi.getTransactionByHash(hashOfTx));
        assertThat(txObj).isEqualTo(txapi.getTransactionByBlockHashAndIndex(hashOfBlock, txIndexPosition));
        assertThat(txObj).isEqualTo(txapi.getTransactionByBlockNumberAndIndex(blockNumber, txIndexPosition));
        assertThat(txReceiptObj).isEqualTo(txapi.getTransactionReceipt(hashOfTx));

        assertThat(6).isEqualTo(txapi.newPendingTransactionFilter());

        System.out.println("================================================================================");
        System.out.println("txObj : " + txObj);
        System.out.println("txReceiptObj : " + txReceiptObj);
        System.out.println("================================================================================");

//        TransactionDto txdto = new TransactionDto();
//        System.out.println("txdto : " + txdto);
//        Transaction tx = txdto.of(txdto);
//        System.out.println("tx : " + tx);
//        txdto = txdto.createBy(tx);
//        System.out.println("txdto : " + txdto);
//        txdto.setData("hello");
//        txdto.setFrom("rachael");
//        System.out.println(txdto.getData());
//        System.out.println(txdto.getFrom());
//        System.out.println(txdto.getTxHash());
//        System.out.println("tx.toString : " + tx.toString());

//        byte version = 0x00;
//        byte[] type = new byte[7];
//        long timestamp = TimeUtils.time();
//        byte[] from = ByteUtil.hexStringToBytes("04ebe229759df56e461ff5770418b784424782e97d297643e1a75c0a02cc1104737baf9f8adf78bad7fa68dcb7197f843fec31a0fd368fba551c2c0bb224c56caf");
//        byte[] dataHash = ByteUtil.hexStringToBytes("ba5f3ea40e95f49bce11942f375ebd3882eb837976eda5c0cb78b9b99ca7b485");
//        long dataSize = 13;
//        byte[] signature = ByteUtil.hexStringToBytes("1b9b31a1c2af94a41a8f7035d1538c454d838c8decfca323c367c03fafb209137f14ce443f454361ef38475d4d9d3d28ef22a5438376dcfdf9373e0eb360b2f61e");
//        byte[] transactionHash = ByteUtil.hexStringToBytes("b01e540c03e8aca110b84f26912bce780f93f9643fd5f6342a40c593d94e39f2");

//        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
//        tmp.write(version);
//        tmp.write(type);
//        tmp.write(ByteUtil.longToBytes(timestamp));
//        tmp.write(from);
//        tmp.write(dataHash);
//        tmp.write(ByteUtil.longToBytes(dataSize));
//        tmp.write(signature);

/*
TransactionHeader {
    version=0,
    type=00000000000000,
    timestamp=162784948603209,
    from=04ebe229759df56e461ff5770418b784424782e97d297643e1a75c0a02cc1104737baf9f8adf78bad7fa68dcb7197f843fec31a0fd368fba551c2c0bb224c56caf,
    dataHash=ba5f3ea40e95f49bce11942f375ebd3882eb837976eda5c0cb78b9b99ca7b485,
    dataSize=13,
    signature=1b9b31a1c2af94a41a8f7035d1538c454d838c8decfca323c367c03fafb209137f14ce443f454361ef38475d4d9d3d28ef22a5438376dcfdf9373e0eb360b2f61e,
    transactionHash=b01e540c03e8aca110b84f26912bce780f93f9643fd5f6342a40c593d94e39f2
}
transactionData={
        "data":null
}
*/

//        TransactionMock txMock = new TransactionMock();
//        System.out.println(txMock.retTxMock().toString());
//
//        TransactionReceiptMock txReceiptMock = new TransactionReceiptMock();
//        System.out.println(txReceiptMock.retTxReceiptMock().toString());
    }
}



