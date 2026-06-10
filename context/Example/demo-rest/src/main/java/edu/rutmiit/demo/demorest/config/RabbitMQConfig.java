package edu.rutmiit.demo.demorest.config;

import edu.rutmiit.demo.events.RoutingKeys;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Конфигурация RabbitMQ на стороне publisher'а (demo-rest).
 *
 * Publisher'у нужно знать только exchange — куда отправлять сообщения.
 * Очереди и привязки — ответственность consumer'а (audit-service).
 *
 * Это промышленный принцип: producer не знает, сколько consumer'ов подписано
 * и какие очереди они используют. Он просто публикует событие в exchange
 * с нужным routing key.
 */
@Configuration
public class RabbitMQConfig {

    /**
     * JSON-конвертер для сериализации доменных событий в JSON.
     *
     * JacksonJsonMessageConverter — актуальная реализация для Spring AMQP 4.0+
     * (замена устаревшему Jackson2JsonMessageConverter).
     *
     * Принимаем настроенный ObjectMapper из Spring Boot — он уже поддерживает
     * java.time (Instant/LocalDate) через автоконфигурацию Jackson 3.
     */
    @Bean
    public MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    /**
     * RabbitTemplate — фасад для отправки сообщений в RabbitMQ.
     *
     * Подключаем JSON-конвертер, чтобы вызовы convertAndSend() автоматически
     * сериализовали Java-объекты в JSON.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    /**
     * Topic exchange — точка обмена для всех доменных событий.
     *
     * Имя exchange берём из контракта (RoutingKeys.EXCHANGE) — publisher и consumer
     * используют одно и то же имя. Рассогласование имени exchange —
     * распространённая ошибка, которую трудно отладить.
     */
    @Bean
    public TopicExchange eventsExchange() {
        return ExchangeBuilder
                .topicExchange(RoutingKeys.EXCHANGE)
                .durable(true)
                .build();
    }
}
