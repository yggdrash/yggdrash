package io.yggdrash.node.api;

import io.yggdrash.core.Transaction;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@Import(ApplicationConfig.class)
public class TransactionAPITest {

    @Test
    public void test() throws Exception {
//        TransactionAPIImpl txapi = new TransactionAPIImpl();
//
//        assertThat(5).isEqualTo(txapi.test(2,3));
//        assertThat(0).isEqualTo(txapi.getTransactionCount("0x407d73d8a49eeb85d32cf465507dd71d507100c1", "latest"));
//        assertThat(100).isEqualTo(txapi.getTransactionCount("0x407d73d8a49eeb85d32cf465507dd71d507100c1", 100));
//        assertThat(1).isEqualTo(txapi.getBlockTransactionCountByHash());
//        assertThat(2).isEqualTo(txapi.getBlockTransactionCountByNumber());
//        assertThat(3).isEqualTo(txapi.sendTransaction());
//        assertThat(4).isEqualTo(txapi.sendRawTransaction());
//        assertThat(5).isEqualTo(txapi.getTransactionByHash());
//        assertThat(6).isEqualTo(txapi.getTransactionByBlockHashAndIndex());
//        assertThat(7).isEqualTo(txapi.getTransactionByBlockNumberAndIndex());
//        assertThat(8).isEqualTo(txapi.getTransactionReceipt());
//        assertThat(9).isEqualTo(txapi.newPendingTransactionFilter());

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

        TransactionMock txMock = new TransactionMock();
        System.out.println(txMock.retTxMock());

        TransactionReceiptMock txReceiptMock = new TransactionReceiptMock();
        System.out.println(txReceiptMock.retTxReceiptMock());
    }
}
