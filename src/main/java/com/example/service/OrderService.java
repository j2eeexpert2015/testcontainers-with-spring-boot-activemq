package com.example.service;

import com.example.dto.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final JmsTemplate jmsTemplate;

    public OrderService(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void publishOrder(Order order) {
        logger.info("Publishing order to ActiveMQ: {}", order);
        jmsTemplate.convertAndSend("orders.queue", order);
    }
}
