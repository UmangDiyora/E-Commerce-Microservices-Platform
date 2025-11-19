package com.ecommerce.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange Names
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    // Queue Names
    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String PAYMENT_COMPLETED_QUEUE = "payment.completed.queue";
    public static final String PAYMENT_FAILED_QUEUE = "payment.failed.queue";

    // Routing Keys
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String PAYMENT_COMPLETED_ROUTING_KEY = "payment.completed";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .build();
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable(PAYMENT_COMPLETED_QUEUE).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_QUEUE).build();
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue())
                .to(orderExchange())
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentCompletedBinding() {
        return BindingBuilder.bind(paymentCompletedQueue())
                .to(paymentExchange())
                .with(PAYMENT_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue())
                .to(paymentExchange())
                .with(PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
