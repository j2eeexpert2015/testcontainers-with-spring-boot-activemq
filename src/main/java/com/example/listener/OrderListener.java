package com.example.listener;

import com.example.dto.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class OrderListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderListener.class);

    @JmsListener(destination = "${activemq.queues.order-queue}")
    public void handleOrder(Order order) {
        logger.info("Received order: {}", order);

        if (order.getQuantity() <= 0) {
            logger.error("Invalid order received: {}", order);
            // Throwing exception to simulate failure and trigger DLQ
            throw new IllegalArgumentException("Invalid quantity for order: " + order.getId());
        } else {
            logger.info("Successfully processed order: {}", order.getId());
        }
    }
}
