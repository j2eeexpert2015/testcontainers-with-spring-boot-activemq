package com.example.integration;

import com.example.dto.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// Integration test assuming an EXTERNAL ActiveMQ broker is running
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class OrderControllerExternalBrokerIT {

    private static final Logger logger = LoggerFactory.getLogger(OrderControllerExternalBrokerIT.class);
    private static final String ORDERS_API_PATH = "/api/orders";
    private static final String DLQ_NAME = "ActiveMQ.DLQ";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Test
    void testPostValidOrder_ExternalBroker() {
        // Arrange
        Order validOrder = new Order("VALID_EXT_001", "Monitor", 1);
        logger.info("Testing POST request with valid order: {}", validOrder);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(ORDERS_API_PATH, validOrder, String.class);

        // Assert: Check the HTTP response status and body
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("Order created and sent to queue: " + validOrder.getId());
        logger.info("Received response: {}", response.getStatusCode());
        // NOTE: Verification of downstream JMS processing is omitted for simplicity.
    }

    @Test
    void testPostInvalidOrder_ExternalBroker() {
        // Arrange
        Order invalidOrder = new Order("INVALID_EXT_002", "Damaged Keyboard", -1);
        logger.info("Testing POST request with invalid order: {}", invalidOrder);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(ORDERS_API_PATH, invalidOrder, String.class);

        // Assert HTTP Response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("Order created and sent to queue: " + invalidOrder.getId());
        logger.info("Received response: {}", response.getStatusCode());

        // Assert DLQ Processing
        jmsTemplate.setReceiveTimeout(1000);
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    Object dlqMessage = jmsTemplate.receiveAndConvert(DLQ_NAME);
                    logger.info("Checking DLQ... Received: {}", dlqMessage);
                    assertThat(dlqMessage)
                            .as("Message should be received from DLQ '%s'", DLQ_NAME)
                            .isNotNull();
                    assertThat(dlqMessage).isInstanceOf(Order.class);
                    assertThat(((Order) dlqMessage).getId()).isEqualTo(invalidOrder.getId());
                });
        logger.info("Successfully verified invalid order landed in DLQ: {}", invalidOrder.getId());
    }
}