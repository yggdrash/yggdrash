package io.yggdrash.node.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rabbitmq", ignoreUnknownFields = false)
public class RabbitMQProperties {
    //private String exchangeName;
    //private String routingKey;

    @Value("${cp.query.name:unVerifyTx}")
    private String queueName;

    @Value("${cp.host:127.0.0.1}")
    private String host;

    @Value("${cp.port:5672}")
    private int port;

    @Value("${cp.user.name:guest}")
    private String userName ;

    @Value("${cp.password:guest}")
    private String password;

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}