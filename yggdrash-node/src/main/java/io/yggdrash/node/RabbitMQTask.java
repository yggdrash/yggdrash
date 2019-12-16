package io.yggdrash.node;

import com.rabbitmq.client.Channel;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.rabbitMQ.TransactionConsumer;
import io.yggdrash.node.config.RabbitMQProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import java.io.IOException;

public class RabbitMQTask {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQTask.class);

    private static final long DELAY = 5;
    private final Channel channel;
    private final RabbitMQProperties properties;

    @Autowired
    private BranchGroup branchGroup;

    public RabbitMQTask(Channel channel, RabbitMQProperties properties) {
        this.channel = channel;
        this.properties = properties;
        log.info("MQ properties " + properties.getLimit());

        if (properties.isEnable()) {
            init();
        }
    }

    private void init() {
        try {
            channel.queueDeclare(properties.getQueueName(), false, false, false, null);
            // Channel share
            channel.basicQos(100, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Scheduled(cron = "*/2 * * * * *")
    private void transactionConsumer() {
        try {
            if (properties.isEnable()) {
                log.info("transactionConsumer");
                TransactionConsumer consumer = new TransactionConsumer(channel, branchGroup, properties.getLimit());
                channel.basicConsume(properties.getQueueName(), false, consumer);
                log.info("Queue remain " + channel.messageCount(properties.getQueueName()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***
     * Send to Transaction to Queue
     * @param tx
     */
    public void publishTransaction(Transaction tx) {
        try {
            channel.basicPublish("", properties.getQueueName(), null, tx.toBinary());
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

}