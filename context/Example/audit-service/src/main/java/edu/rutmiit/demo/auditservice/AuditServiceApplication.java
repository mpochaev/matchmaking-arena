package edu.rutmiit.demo.auditservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа в audit-service.
 *
 * Это отдельное Spring Boot приложение, работающее на порту 8081.
 * Оно не предоставляет REST/GraphQL API для клиентов — его единственная
 * задача принимать доменные события из RabbitMQ и вести журнал аудита.
 *
 * Запуск: .\mvnw spring-boot:run -pl audit-service -am
 */
@SpringBootApplication
public class AuditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
