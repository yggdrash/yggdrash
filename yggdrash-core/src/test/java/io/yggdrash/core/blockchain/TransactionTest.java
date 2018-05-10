package io.yggdrash.core.blockchain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Transaction.class)
public class TransactionTest {
    @Test
    public void transactionTest() {
        Transaction t = new Transaction();

        assert t.validation();


    }
}
