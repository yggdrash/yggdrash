package io.yggdrash.node;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YggdrashNode {
    public static void main(String[] args) {
        SpringApplication.run(YggdrashNode.class, args);
    }
}