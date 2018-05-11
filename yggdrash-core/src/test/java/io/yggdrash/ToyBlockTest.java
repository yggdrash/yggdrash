package io.yggdrash;

import com.google.gson.JsonObject;
import io.yggdrash.core.*;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ToyBlockTest {

    public Account from;
    public Account to;
    public Transaction tx1;
    public Transaction tx2;
    public Transactions txs;
    public BlockChain bc;
    public Block gbk;
    public Block bk1;
    public Block bk2;

    @Before
    public void setUp() throws Exception {

        // create tx_data1
        JsonObject data1 = new JsonObject();
        data1.addProperty("key", "balance");
        data1.addProperty("operator", "transfer");
        data1.addProperty("value",30);

        // create tx_data2
        JsonObject data2 = new JsonObject();
        data2.addProperty("key", "balance");
        data2.addProperty("operator", "transfer");
        data2.addProperty("value",10);

        // create account
        this.from = new Account();
        this.to = new Account();

        // create sample tx
        this.tx1 = new Transaction(from, to, data1);
        this.tx2 = new Transaction(from, to, data2);

        // create transactions
        List<Transaction> txs_list;
        txs_list = new ArrayList<Transaction>();
        txs_list.add(this.tx1);
        txs_list.add(this.tx2);
        this.txs = new Transactions(txs_list);
    }

    @Test
    public void TransactionGenTest() throws IOException {

        this.tx1.printTransaction();

        System.out.println();

        this.tx2.printTransaction();

        System.out.println();

        this.txs.printTransactions();

        System.out.println();

        // create blockchain with genesis block
        bc = new BlockChain(from);
        bc.printBlockChain();

        for(int i=0; i < 10; i++) {
            // create next block
            bk1 = new Block(from, bc, txs);

            // add next block in blockchain
            bc.add(bk1);
        }

        this.bc.printBlockChain();

    }
}
