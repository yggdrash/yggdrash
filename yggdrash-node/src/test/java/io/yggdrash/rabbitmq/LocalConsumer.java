package io.yggdrash.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionImpl;
import org.apache.commons.logging.Log;

public class LocalConsumer extends DefaultConsumer {
    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */

    private Channel channel;
    private Log log;

    public LocalConsumer(Channel channel, Log logger) {
        super(channel);
        this.log = logger;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {
        Transaction tx = new TransactionImpl(body);
        // insert here whatever logic is needed, using whatever service you injected - in this case it is a simple logger.
        log.debug("Tx Hash : " + tx.getHash().toString());
    }

}
