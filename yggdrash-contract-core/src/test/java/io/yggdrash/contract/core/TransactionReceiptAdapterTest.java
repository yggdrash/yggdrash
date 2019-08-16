package io.yggdrash.contract.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TransactionReceiptAdapterTest {


    @Test
    public void testTransactionReceipt() {
        TransactionReceipt tr = new TransactionReceiptImpl();

        tr.setIssuer("test");
        tr.setStatus(ExecuteStatus.ERROR);
        tr.addLog("LOG1");
        tr.setBlockHeight(1000L);

        TransactionReceiptAdapter adapter = new TransactionReceiptAdapter();
        adapter.setTransactionReceipt(tr);


        adapter.setIssuer("test111");
        adapter.setStatus(ExecuteStatus.SUCCESS);
        adapter.addLog("LOG2");
        adapter.setBlockHeight(2000L);

        assertEquals("test", tr.getIssuer());

        assert adapter.getStatus() == ExecuteStatus.SUCCESS;
        assert adapter.getTxLog().size() == 2;
        assert adapter.getBlockHeight() == 1000L;



    }

}