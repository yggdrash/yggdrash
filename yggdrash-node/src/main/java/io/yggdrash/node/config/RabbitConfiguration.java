package io.yggdrash.node.config;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.yggdrash.node.RabbitMQTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMQProperties.class)
public class RabbitConfiguration {

    private static final Logger log = LoggerFactory.getLogger(P2PConfiguration.class);

    private final RabbitMQProperties rabbitMQProperties;

    private Connection connection;
    private Channel channel;

    @Autowired
    RabbitConfiguration(RabbitMQProperties rabbitMQProperties) {
        this.rabbitMQProperties = rabbitMQProperties;
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMQProperties.getHost());
        factory.setPort(rabbitMQProperties.getPort());
        factory.setUsername(rabbitMQProperties.getUserName());
        factory.setPassword(rabbitMQProperties.getPassword());
        return factory;
    }

    @Bean
    public Connection connection(ConnectionFactory factory) throws Exception {
        return factory.newConnection();
    }

    @Bean
    public Channel channel(Connection connection) throws Exception {
        return connection.createChannel();
    }

    @Bean
    RabbitMQTask rabbitMQTask(Channel channel, RabbitMQProperties properties) {
        return new RabbitMQTask(channel, properties);
    }
}