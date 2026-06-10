package com.example.demo.config;

import edu.rutmiit.demo.matchmakingevents.RoutingKeys;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * RabbitMQ-конфигурация основного сервиса.
 * Сервис публикует события и слушает match.enriched для обновления рейтингов игроков.
 */
@Configuration
public class RabbitMQConfig {

    public static final String MATCH_ENRICHED_QUEUE = "q.matchmaking.players.match-enriched";
    public static final String MATCH_ENRICHED_DLQ = "q.matchmaking.players.match-enriched.dlq";

    @Bean
    public MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }


    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(3);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public TopicExchange eventsExchange() {
        return ExchangeBuilder
                .topicExchange(RoutingKeys.EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
                .directExchange(RoutingKeys.EXCHANGE + ".dlx")
                .durable(true)
                .build();
    }

    @Bean
    public Queue matchEnrichedQueue() {
        return QueueBuilder
                .durable(MATCH_ENRICHED_QUEUE)
                .deadLetterExchange(RoutingKeys.EXCHANGE + ".dlx")
                .deadLetterRoutingKey(MATCH_ENRICHED_DLQ)
                .build();
    }

    @Bean
    public Queue matchEnrichedDlq() {
        return QueueBuilder.durable(MATCH_ENRICHED_DLQ).build();
    }

    @Bean
    public Binding matchEnrichedBinding(Queue matchEnrichedQueue, TopicExchange eventsExchange) {
        return BindingBuilder
                .bind(matchEnrichedQueue)
                .to(eventsExchange)
                .with(RoutingKeys.MATCH_ENRICHED);
    }

    @Bean
    public Binding matchEnrichedDlqBinding(Queue matchEnrichedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder
                .bind(matchEnrichedDlq)
                .to(deadLetterExchange)
                .with(MATCH_ENRICHED_DLQ);
    }
}
