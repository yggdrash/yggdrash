package io.yggdrash.node;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQTestUtils {

    private static final String queryName = "unVerifyTx";
    private static final String userName = "guest";
    private static final String password = "guest";
    private static final String host = "localhost";
    private static final int port = 5672;

    private static Channel channel;

    static {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(userName);
            factory.setPassword(password);

            Connection connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(queryName, false, false, false, null);
        } catch (TimeoutException | IOException e) {
            e.printStackTrace();
        }
    }

    static Channel getChannel() {
        return channel;
    }

    static String getQueryName() {
        return queryName;
    }
}