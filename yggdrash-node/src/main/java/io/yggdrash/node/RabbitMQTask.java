package io.yggdrash.node;

import com.rabbitmq.client.Channel;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.rabbitMQ.TransactionConsumer;
import io.yggdrash.node.config.RabbitMQProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

public class RabbitMQTask {

    private static final long DELAY = 5;
    private final Channel channel;
    private final RabbitMQProperties properties;

    @Autowired
    private BranchGroup branchGroup;

    public RabbitMQTask(Channel channel, RabbitMQProperties properties) {
        this.channel = channel;
        this.properties = properties;
        init();
    }

    private void init() {
        try {
            channel.queueDeclare(properties.getQueueName(), false, false, false, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Scheduled(cron = "*/" + DELAY + " * * * * *")
    private void transactionConsumer() {
        try {
            TransactionConsumer consumer = new TransactionConsumer(channel, branchGroup);
            channel.basicConsume("unVerifyTx", true, consumer);
            /*
            for (int i = 0; i < LIMIT; i++) {
                channel.basicConsume("unVerifyTx", true, consumer);
            }
            */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}