package com.ecommerce.report.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    //Exchange values
    @Value("${messaging.exchange.order}")
    private String orderExchange;

    @Value("${messaging.exchange.inventory}")
    private String inventoryExchange;

    @Value("${messaging.exchange.payment}")
    private String paymentExchange;

    //Queue values
    @Value("${messaging.queue.order-created}")
    private String orderCreatedQueue;

    @Value("${messaging.queue.order-created-retry}")
    private String orderCreatedRetryQueue;

    @Value("${messaging.queue.order-created-dlq}")
    private String orderCreatedDeadQueue;

    @Value("${messaging.queue.order-cancelled}")
    private String orderCancelledQueue;

    @Value("${messaging.queue.order-cancelled-retry}")
    private String orderCancelledRetryQueue;

    @Value("${messaging.queue.order-cancelled-dlq}")
    private String orderCancelledDeadQueue;

    @Value("${messaging.queue.payment-completed}")
    private String paymentCompletedQueue;

    @Value("${messaging.queue.payment-completed-retry}")
    private String paymentCompletedRetryQueue;

    @Value("${messaging.queue.payment-completed-dlq}")
    private String paymentCompletedDeadQueue;

    @Value("${messaging.queue.payment-failed}")
    private String paymentFailedQueue;

    @Value("${messaging.queue.payment-failed-retry}")
    private String paymentFailedRetryQueue;

    @Value("${messaging.queue.payment-failed-dlq}")
    private String paymentFailedDeadQueue;

    @Value("${messaging.queue.stock-reserved}")
    private String stockReservedQueue;

    @Value("${messaging.queue.stock-reserved-retry}")
    private String stockReservedRetryQueue;

    @Value("${messaging.queue.stock-reserved-dlq}")
    private String stockReservedDeadQueue;

    @Value("${messaging.queue.stock-reservation-failed}")
    private String stockReservationFailedQueue;

    @Value("${messaging.queue.stock-reservation-failed-retry}")
    private String stockReservationFailedRetryQueue;

    @Value("${messaging.queue.stock-reservation-failed-dlq}")
    private String stockReservationFailedDeadQueue;

    //Routing key values
    @Value("${messaging.routing-key.order-created}")
    private String orderCreatedKey;

    @Value("${messaging.routing-key.order-cancelled}")
    private String orderCancelledKey;

    @Value("${messaging.routing-key.payment-completed}")
    private String paymentCompletedKey;

    @Value("${messaging.routing-key.payment-failed}")
    private String paymentFailedKey;

    @Value("${messaging.routing-key.stock-reserved}")
    private String stockReservedKey;

    @Value("${messaging.routing-key.stock-reservation-failed}")
    private String stockReservationFailedKey;

    // Exchanges
    @Bean
    public TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange(orderExchange).durable(true).build();
    }
    @Bean
    public TopicExchange orderRetryExchange() {
        return ExchangeBuilder.topicExchange(orderExchange+".retry").durable(true).build();
    }

    @Bean
    public TopicExchange orderDeadLetterExchange() {
        return ExchangeBuilder.topicExchange(orderExchange + ".dlx").durable(true).build();
    }

    @Bean
    public TopicExchange paymentExchange() {
        return ExchangeBuilder.topicExchange(paymentExchange).durable(true).build();
    }

    @Bean
    public TopicExchange paymentRetryExchange() {
        return ExchangeBuilder.topicExchange(paymentExchange + ".retry").durable(true).build();
    }

    @Bean
    public TopicExchange paymentDeadLetterExchange() {
        return ExchangeBuilder.topicExchange(paymentExchange + ".dlx").durable(true).build();
    }

    @Bean
    public TopicExchange inventoryExchange() {
        return ExchangeBuilder.topicExchange(inventoryExchange).durable(true).build();
    }

    @Bean
    public TopicExchange inventoryRetryExchange() {
        return ExchangeBuilder.topicExchange(inventoryExchange + ".retry").durable(true).build();
    }

    @Bean
    public TopicExchange inventoryDeadLetterExchange() {
        return ExchangeBuilder.topicExchange(inventoryExchange + ".dlx").durable(true).build();
    }

    //Queues
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(orderCreatedQueue)
                .withArgument("x-dead-letter-exchange", orderExchange+".retry")
                .withArgument("x-dead-letter-routing-key", orderCreatedKey+".retry")
                .build();
    }

    @Bean
    public Queue orderCreatedRetryQueue() {
        return QueueBuilder.durable(orderCreatedRetryQueue)
                .withArgument("x-message-ttl", 10000)
                .withArgument("x-dead-letter-exchange", orderExchange)
                .withArgument("x-dead-letter-routing-key", orderCreatedKey)
                .build();
    }

    @Bean
    public Queue orderCreatedDlqQueue() {
        return QueueBuilder.durable(orderCreatedDeadQueue).build();
    }

    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder.durable(orderCancelledQueue)
                .withArgument("x-dead-letter-exchange", orderExchange+".retry")
                .withArgument("x-dead-letter-routing-key", orderCancelledKey+".retry")
                .build();
    }

    @Bean
    public Queue orderCancelledRetryQueue() {
        return QueueBuilder.durable(orderCancelledRetryQueue)
                .withArgument("x-message-ttl", 10000)
                .withArgument("x-dead-letter-exchange", orderExchange)
                .withArgument("x-dead-letter-routing-key", orderCancelledKey)
                .build();
    }

    @Bean
    public Queue orderCancelledDlqQueue() {
        return QueueBuilder.durable(orderCancelledDeadQueue).build();
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable(paymentCompletedQueue)
                .withArgument("x-dead-letter-exchange", paymentExchange + ".retry")
                .withArgument("x-dead-letter-routing-key", paymentCompletedKey + ".retry")
                .build();
    }

    @Bean
    public Queue paymentCompletedRetryQueue() {
        return QueueBuilder.durable(paymentCompletedRetryQueue)
                .withArgument("x-message-ttl", 10000)
                .withArgument("x-dead-letter-exchange", paymentExchange)
                .withArgument("x-dead-letter-routing-key", paymentCompletedKey)
                .build();
    }

    @Bean
    public Queue paymentCompletedDlqQueue() {
        return QueueBuilder.durable(paymentCompletedDeadQueue).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(paymentFailedQueue)
                .withArgument("x-dead-letter-exchange", paymentExchange + ".retry")
                .withArgument("x-dead-letter-routing-key", paymentFailedKey + ".retry")
                .build();
    }

    @Bean
    public Queue paymentFailedRetryQueue() {
        return QueueBuilder.durable(paymentFailedRetryQueue)
                .withArgument("x-message-ttl", 10000)
                .withArgument("x-dead-letter-exchange", paymentExchange)
                .withArgument("x-dead-letter-routing-key", paymentFailedKey)
                .build();
    }

    @Bean
    public Queue paymentFailedDlqQueue() {
        return QueueBuilder.durable(paymentFailedDeadQueue).build();
    }

    @Bean
    public Queue stockReservedQueue() {
        return QueueBuilder.durable(stockReservedQueue)
                .withArgument("x-dead-letter-exchange", inventoryExchange + ".retry")
                .withArgument("x-dead-letter-routing-key", stockReservedKey + ".retry")
                .build();
    }

    @Bean
    public Queue stockReservedRetryQueue() {
        return QueueBuilder.durable(stockReservedRetryQueue)
                .withArgument("x-message-ttl", 10000)
                .withArgument("x-dead-letter-exchange", inventoryExchange)
                .withArgument("x-dead-letter-routing-key", stockReservedKey)
                .build();
    }

    @Bean
    public Queue stockReservedDlqQueue() {
        return QueueBuilder.durable(stockReservedDeadQueue).build();
    }

    @Bean
    public Queue stockReservationFailedQueue() {
        return QueueBuilder.durable(stockReservationFailedQueue)
                .withArgument("x-dead-letter-exchange", inventoryExchange + ".retry")
                .withArgument("x-dead-letter-routing-key", stockReservationFailedKey + ".retry")
                .build();
    }

    @Bean
    public Queue stockReservationFailedRetryQueue() {
        return QueueBuilder.durable(stockReservationFailedRetryQueue)
                .withArgument("x-message-ttl", 10000)
                .withArgument("x-dead-letter-exchange", inventoryExchange)
                .withArgument("x-dead-letter-routing-key", stockReservationFailedKey)
                .build();
    }

    @Bean
    public Queue stockReservationFailedDlqQueue() {
        return QueueBuilder.durable(stockReservationFailedDeadQueue).build();
    }

    // Bindings
    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue()).to(orderExchange()).with(orderCreatedKey);
    }

    @Bean
    public Binding orderCreatedRetryBinding() {
        return BindingBuilder.bind(orderCreatedRetryQueue()).to(orderRetryExchange()).with(orderCreatedKey+".retry");
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder.bind(orderCancelledQueue()).to(orderExchange()).with(orderCancelledKey);
    }

    @Bean
    public Binding orderCancelledRetryBinding() {
        return BindingBuilder.bind(orderCancelledRetryQueue()).to(orderRetryExchange()).with(orderCancelledKey+".retry");
    }

    @Bean
    public Binding paymentCompletedBinding() {
        return BindingBuilder.bind(paymentCompletedQueue()).to(paymentExchange()).with(paymentCompletedKey);
    }

    @Bean
    public Binding paymentCompletedRetryBinding() {
        return BindingBuilder.bind(paymentCompletedRetryQueue()).to(paymentRetryExchange()).with(paymentCompletedKey + ".retry");
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue()).to(paymentExchange()).with(paymentFailedKey);
    }
    @Bean
    public Binding paymentFailedRetryBinding() {
        return BindingBuilder.bind(paymentFailedRetryQueue()).to(paymentRetryExchange()).with(paymentFailedKey + ".retry");
    }

    @Bean
    public Binding stockReservedBinding() {
        return BindingBuilder.bind(stockReservedQueue()).to(inventoryExchange()).with(stockReservedKey);
    }

    @Bean
    public Binding stockReservedRetryBinding() {
        return BindingBuilder.bind(stockReservedRetryQueue()).to(inventoryRetryExchange()).with(stockReservedKey + ".retry");
    }

    @Bean
    public Binding stockReservationFailedBinding() {
        return BindingBuilder.bind(stockReservationFailedQueue()).to(inventoryExchange()).with(stockReservationFailedKey);
    }

    @Bean
    public Binding stockReservationFailedRetryBinding() {
        return BindingBuilder.bind(stockReservationFailedRetryQueue()).to(inventoryRetryExchange()).with(stockReservationFailedKey + ".retry");
    }

    @Bean
    public Binding orderCreatedDlqBinding() {
        return BindingBuilder.bind(orderCreatedDlqQueue())
                .to(orderDeadLetterExchange())
                .with(orderCreatedKey + ".dead");
    }

    @Bean
    public Binding orderCancelledDlqBinding() {
        return BindingBuilder.bind(orderCancelledDlqQueue())
                .to(orderDeadLetterExchange())
                .with(orderCancelledKey + ".dead");
    }

    @Bean
    public Binding paymentCompletedDlqBinding() {
        return BindingBuilder.bind(paymentCompletedDlqQueue())
                .to(paymentDeadLetterExchange())
                .with(paymentCompletedKey + ".dead");
    }

    @Bean
    public Binding paymentFailedDlqBinding() {
        return BindingBuilder.bind(paymentFailedDlqQueue())
                .to(paymentDeadLetterExchange())
                .with(paymentFailedKey + ".dead");
    }

    @Bean
    public Binding stockReservedDlqBinding() {
        return BindingBuilder.bind(stockReservedDlqQueue())
                .to(inventoryDeadLetterExchange())
                .with(stockReservedKey + ".dead");
    }

    @Bean
    public Binding stockReservationFailedDlqBinding() {
        return BindingBuilder.bind(stockReservationFailedDlqQueue())
                .to(inventoryDeadLetterExchange())
                .with(stockReservationFailedKey + ".dead");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }
}