package com.example.config;

import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter; // Added import
import org.springframework.jms.support.converter.MessageConverter;             // Added import
import org.springframework.jms.support.converter.MessageType;                // Added import
import org.springframework.util.ErrorHandler;

import jakarta.jms.ConnectionFactory; // Ensure this matches your Spring Boot version (jakarta or javax)
import jakarta.jms.Queue;

@Configuration
public class ActiveMQConfig {

    private static final Logger logger = LoggerFactory.getLogger(ActiveMQConfig.class);

    // Original bean for defining the queue
    @Bean
    public Queue orderQueue() {
        return new ActiveMQQueue("orders.queue");
    }

    // Bean for the custom JMS Error Handler
    @Bean
    public ErrorHandler jmsErrorHandler() {
        return t -> {
            // Log the error without throwing it again from the error handler
            logger.error("In jmsErrorHandler -> Error processing JMS message: {}", t.getMessage(), t); // Log full stack trace
        };
    }

    // Bean for configuring the JMS Listener Container Factory to use the error handler
    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            DefaultJmsListenerContainerFactoryConfigurer configurer,
            ErrorHandler jmsErrorHandler) { // Inject the custom ErrorHandler

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        // Configure the factory using Spring Boot defaults first
        configurer.configure(factory, connectionFactory);
        // Set the custom error handler
        factory.setErrorHandler(jmsErrorHandler);
        logger.info("Configured JMS Listener Container Factory with custom ErrorHandler.");
        return factory;
    }

    // Bean for configuring Jackson Message converter for JSON text messages
    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        // Set target type to TextMessage
        converter.setTargetType(MessageType.TEXT);
        // Set typeId property name (_type by default) to allow listener to map back to Order class
        converter.setTypeIdPropertyName("_type");
        logger.info("Configured Jackson Message Converter for JMS.");
        return converter;
    }
}