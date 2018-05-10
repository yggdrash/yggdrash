package io.yggdrash;

import com.google.gson.JsonObject;
import io.yggdrash.core.Account;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.Transactions;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ToyBlockTest {

    public Account from;
    public Account to;
    public Transaction tx1;
    public Transaction tx2;
    public Transactions txs;

    @Before
    public void setUp() throws Exception {

        JsonObject data1 = new JsonObject();
        data1.addProperty("key", "balance");
        data1.addProperty("operator", "transfer");
        data1.addProperty("value",30);

        JsonObject data2 = new JsonObject();
        data2.addProperty("key", "balance");
        data2.addProperty("operator", "transfer");
        data2.addProperty("value",10);

        this.from = new Account();
        this.to = new Account();
        this.tx1 = new Transaction(from, to, data1);
        this.tx2 = new Transaction(from, to, data2);

        List<Transaction> txs_list;
        txs_list = new ArrayList<Transaction>();
        txs_list.add(this.tx1);
        txs_list.add(this.tx2);

        this.txs = new Transactions(txs_list);

    }

    @Test
    public void TransactionGenTest() {

        this.tx1.printTransaction();

        System.out.println();

        this.tx2.printTransaction();

        System.out.println();

        this.txs.printTransactions();
    }
}
