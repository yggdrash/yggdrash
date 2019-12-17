package io.yggdrash.core.rabbitMQ;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.yggdrash.core.blockchain.BlockChainManager;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class TransactionConsumer extends DefaultConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionConsumer.class);

    private BranchGroup branchGroup;

    private int qos = 0;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */
    public TransactionConsumer(Channel channel, BranchGroup branchGroup, int qos) {
        super(channel);
        this.branchGroup = branchGroup;
        this.qos = qos;
    }

    public TransactionConsumer(Channel channel, BranchGroup branchGroup) {
        super(channel);
        this.branchGroup = branchGroup;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {
        String routingKey = envelope.getRoutingKey();
        String exchange = envelope.getExchange();
        long deliveryTag = envelope.getDeliveryTag();
        Transaction tx = new TransactionImpl(body);
        BranchId branch = tx.getBranchId();
        BlockChainManager manager = branchGroup.getBranch(branch).getBlockChainManager();
        // check exist
        if (manager.contains(tx)) {
            this.getChannel().basicAck(deliveryTag, true);
            return;
        }
        // qos == 0 is unlimited
        if (qos == 0) {
            branchGroup.addTransaction(tx);
            log.debug("HandleDelivery : txHash={}, routingKey={}, exchange={}, deliveryTag={}",
                    routingKey, exchange, deliveryTag, tx.getHash().toString());
            this.getChannel().basicAck(deliveryTag, true);
        } else {
            int unconfirmedTxSize = manager.getUnconfirmedTxSize();
            log.debug("UnconfirmedTxSize Tx Size " + unconfirmedTxSize);
            // qos > 0 is limit unconfirmedTxSize
            if (unconfirmedTxSize < qos) {
                branchGroup.addTransaction(tx);
                this.getChannel().basicAck(deliveryTag, true);
            } else {
                // requeue
                this.getChannel().basicReject(deliveryTag, true);
            }
        }

    }
}