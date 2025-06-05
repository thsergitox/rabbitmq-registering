package com.registration.userservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.queues.persist}")
    private String persistQueue;

    @Value("${rabbitmq.queues.response}")
    private String responseQueue;

    @Value("${rabbitmq.routing-keys.persist}")
    private String persistRoutingKey;

    @Value("${rabbitmq.routing-keys.persisted}")
    private String persistedRoutingKey;

    @Bean
    public DirectExchange registroExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Queue persistQueue() {
        return QueueBuilder.durable(persistQueue).build();
    }

    @Bean
    public Queue responseQueue() {
        return QueueBuilder.durable(responseQueue).build();
    }

    @Bean
    public Binding persistBinding() {
        return BindingBuilder
                .bind(persistQueue())
                .to(registroExchange())
                .with(persistRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
} 