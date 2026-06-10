package edu.rutmiit.demo.matchlogservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Отдельный сервис-потребитель читает события из RabbitMQ и пишет матч-лог. */
@SpringBootApplication
public class MatchLogServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MatchLogServiceApplication.class, args);
    }
}
