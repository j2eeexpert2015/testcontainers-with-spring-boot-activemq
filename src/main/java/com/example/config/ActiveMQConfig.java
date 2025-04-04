package com.example.config;

import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.jms.Queue;

@Configuration
public class ActiveMQConfig {

    @Bean
    public Queue orderQueue() {
        return new ActiveMQQueue("orders.queue");
    }
}
