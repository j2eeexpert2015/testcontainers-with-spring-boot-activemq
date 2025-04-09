package com.example.integration;

import com.example.dto.Order;
// Listener import not needed for verification in this version
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class OrderControllerTestContainersIT {

    private static final Logger logger = LoggerFactory.getLogger(OrderControllerTestContainersIT.class);
    private static final String ORDERS_API_PATH = "/api/orders";
    private static final String DLQ_NAME = "ActiveMQ.DLQ";

    @Container
    static GenericContainer<?> activeMQ = new GenericContainer<>(DockerImageName.parse("rmohr/activemq:5.15.9"))
            .withExposedPorts(61616);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.activemq.broker-url",
                () -> "tcp://" + activeMQ.getHost() + ":" + activeMQ.getMappedPort(61616));
        registry.add("spring.activemq.user", () -> "admin");
        registry.add("spring.activemq.password", () -> "admin");
        logger.info("Overriding ActiveMQ properties for Testcontainers: {}", registry.toString());
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Test
    void testPostValidOrder() {
        // Arrange: Create a valid order
        Order validOrder = new Order("VALID_TC_001", "Laptop", 1);
        logger.info("Testing POST request with valid order: {}", validOrder);

        // Act: Send POST request to the controller
        ResponseEntity<String> response = restTemplate.postForEntity(ORDERS_API_PATH, validOrder, String.class);

        // Assert: Check the HTTP response status and body
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("Order created and sent to queue: " + validOrder.getId());
        logger.info("Received response: {}", response.getStatusCode());
    }

    @Test
    void testPostInvalidOrder() {
        // Arrange: Create an invalid order (quantity = 0)
        Order invalidOrder = new Order("INVALID_TC_002", "Empty Box", 0);
        logger.info("Testing POST request with invalid order: {}", invalidOrder);

        // Act: Send POST request to the controller
        ResponseEntity<String> response = restTemplate.postForEntity(ORDERS_API_PATH, invalidOrder, String.class);

        // Assert: Check HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("Order created and sent to queue: " + invalidOrder.getId());
        logger.info("Received response: {}", response.getStatusCode());

        // Assert: Verify the message eventually lands in the DLQ using Awaitility
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