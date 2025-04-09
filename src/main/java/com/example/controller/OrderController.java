package com.example.controller;

import com.example.dto.Order;
import com.example.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders") // Base path for order-related endpoints
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    // Inject the OrderService
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody Order order) {
        try {
            logger.info("Received request to create order: {}", order);
            orderService.publishOrder(order); // Use the existing service to send the message
            logger.info("Order successfully published to the queue: {}", order.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body("Order created and sent to queue: " + order.getId());
        } catch (Exception e) {
            logger.error("Error publishing order: {}", order.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating order: " + e.getMessage());
        }
    }
}