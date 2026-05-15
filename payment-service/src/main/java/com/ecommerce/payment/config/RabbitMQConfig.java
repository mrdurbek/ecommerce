package com.ecommerce.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${messaging.exchange.payment}")
    private String paymentExchange;

//    @Value("${messaging.queue.payment-completed}")
//    private String completedQueue;
//
//    @Value("${messaging.queue.payment-completed-retry}")
//    private String completedRetryQueue;
//
//    @Value("${messaging.queue.payment-completed-dlq}")
//    private String completedDlq;
//
//    @Value("${messaging.queue.payment-failed}")
//    private String failedQueue;
//
//    @Value("${messaging.queue.payment-failed-retry}")
//    private String failedRetryQueue;
//
//    @Value("${messaging.queue.payment-failed-dlq}")
//    private String failedDlq;
//
//    @Value("${messaging.routing-key.payment-completed}")
//    private String completedKey;
//
//    @Value("${messaging.routing-key.payment-failed}")
//    private String failedKey;

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

//    @Bean
//    public Queue paymentCompletedQueue() {
//        return QueueBuilder.durable(completedQueue)
//                .withArgument("x-dead-letter-exchange", paymentExchange + ".retry")
//                .withArgument("x-dead-letter-routing-key", completedKey + ".retry")
//                .build();
//    }
//
//    @Bean
//    public Queue paymentCompletedRetryQueue() {
//        return QueueBuilder.durable(completedRetryQueue)
//                .withArgument("x-message-ttl", 10000)
//                .withArgument("x-dead-letter-exchange", paymentExchange)
//                .withArgument("x-dead-letter-routing-key", completedKey)
//                .build();
//    }
//
//    @Bean
//    public Queue paymentFailedQueue() {
//        return QueueBuilder.durable(failedQueue)
//                .withArgument("x-dead-letter-exchange", paymentExchange + ".retry")
//                .withArgument("x-dead-letter-routing-key", failedKey + ".retry")
//                .build();
//    }
//
//    @Bean
//    public Queue paymentFailedRetryQueue() {
//        return QueueBuilder.durable(failedRetryQueue)
//                .withArgument("x-message-ttl", 10000)
//                .withArgument("x-dead-letter-exchange", paymentExchange)
//                .withArgument("x-dead-letter-routing-key", failedKey)
//                .build();
//    }
//
//    @Bean
//    public Queue paymentCompletedDlqQueue() {
//        return QueueBuilder.durable(completedDlq).build();
//    }
//
//    @Bean
//    public Queue paymentFailedDlqQueue() {
//        return QueueBuilder.durable(failedDlq).build();
//    }
//
//    @Bean
//    public Binding completedBinding() {
//        return BindingBuilder.bind(paymentCompletedQueue())
//                .to(paymentExchange())
//                .with(completedKey);
//    }
//
//    @Bean
//    public Binding completedRetryBinding() {
//        return BindingBuilder.bind(paymentCompletedRetryQueue())
//                .to(paymentRetryExchange())
//                .with(completedKey + ".retry");
//    }
//
//    @Bean
//    public Binding failedBinding() {
//        return BindingBuilder.bind(paymentFailedQueue())
//                .to(paymentExchange())
//                .with(failedKey);
//    }
//
//    @Bean
//    public Binding failedRetryBinding() {
//        return BindingBuilder.bind(paymentFailedRetryQueue())
//                .to(paymentRetryExchange())
//                .with(failedKey + ".retry");
//    }
//
//    @Bean
//    public Binding completedDlqBinding() {
//        return BindingBuilder.bind(paymentCompletedDlqQueue())
//                .to(paymentDeadLetterExchange())
//                .with(completedKey + ".dead");
//    }
//
//    @Bean
//    public Binding failedDlqBinding() {
//        return BindingBuilder.bind(paymentFailedDlqQueue())
//                .to(paymentDeadLetterExchange())
//                .with(failedKey + ".dead");
//    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate template(ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }
}
