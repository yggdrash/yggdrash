package io.yggdrash.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.wallet.Wallet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.crypto.InvalidCipherTextException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RabbitMqTest {
    Log log = LogFactory.getLog(RabbitMqTest.class);
    private final static String QUEUE_NAME = "test";
    private final static String QUEUE_NAME_TRANSACTION = "unVerifyTx";
    private Connection connection;
    private Channel channel;
    private Wallet wallet;

    @Before
    public void init() throws IOException, InvalidCipherTextException {
        ConnectionFactory factory = new ConnectionFactory();

        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");

        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            channel.queueDeclare(QUEUE_NAME_TRANSACTION, false,false, false, null);
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        wallet = new Wallet(new DefaultConfig(), "Aa1234567890!");
    }


    @Test
    public void sendQueue() {
        log.debug("sendQueue");
        try {

            String message = "TEST";
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
            log.debug("sendQueue Done");

        } catch (Exception e) {
            log.debug(e.getMessage());
        }

    }

    @Test
    public void sendTransactionTest() {
        try {
            long queueCount = channel.messageCount(QUEUE_NAME_TRANSACTION);
            List<Transaction> txList = new ArrayList<Transaction>();
            int createTxCount = 1000;
            log.debug("Create Tx");
            for(int i=0;i<createTxCount;i++) {
                Transaction tx = BlockChainTestUtils.createBranchTx();
                txList.add(tx);
            }
            log.debug("Create Tx Done");
            log.debug("publish");
            for(Transaction tx:txList) {
                channel.basicPublish("", QUEUE_NAME_TRANSACTION, null, tx.toBinary());
            }
            log.debug("publish Done");
            queueCount = channel.messageCount(QUEUE_NAME_TRANSACTION) - queueCount;
            assertEquals("Push queue Tx Count", queueCount, createTxCount);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void receiveTransactionTest() {
        try {
            Consumer consumer = new LocalConsumer(channel, log);
            while (channel.messageCount(QUEUE_NAME_TRANSACTION) > 0) {
                channel.basicConsume(QUEUE_NAME_TRANSACTION, true, consumer);
            }

            assertTrue("", channel.messageCount(QUEUE_NAME_TRANSACTION) == 0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
