package com.example.integration;

import com.example.dto.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Testcontainers
public class ActiveMQIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(ActiveMQIntegrationTest.class);

    @Container
    static GenericContainer<?> activeMQ = new GenericContainer<>("rmohr/activemq:5.15.9")
            .withExposedPorts(61616);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.activemq.broker-url",
                () -> "tcp://" + activeMQ.getHost() + ":" + activeMQ.getMappedPort(61616));
        registry.add("spring.activemq.user", () -> "admin");
        registry.add("spring.activemq.password", () -> "admin");
    }

    @Autowired
    private JmsTemplate jmsTemplate;

    @Test
    void testSendAndReceiveOrder() {
        Order order = new Order("789", "Tablet", 2);
        logger.info("Sending test order: {}", order);
        jmsTemplate.convertAndSend("orders.queue", order);

        jmsTemplate.setReceiveTimeout(10000);
        Order remainingMessage = (Order) jmsTemplate.receiveAndConvert("orders.queue");

        logger.info("Remaining message in queue: {}", remainingMessage);
        assertNull(remainingMessage, "Queue should be empty after listener consumes the message");
    }

    @Test
    void testInvalidOrderGoesToDlq() {
        Order invalidOrder = new Order("998", "Phone", 0);
        logger.info("Sending invalid order: {}", invalidOrder);
        jmsTemplate.convertAndSend("orders.queue", invalidOrder);

        jmsTemplate.setReceiveTimeout(10000);
        Order dlqMessage = (Order) jmsTemplate.receiveAndConvert("ActiveMQ.DLQ");

        logger.info("Message in DLQ: {}", dlqMessage);
        assertNotNull(dlqMessage);
        assertEquals("998", dlqMessage.getId());
    }
}
