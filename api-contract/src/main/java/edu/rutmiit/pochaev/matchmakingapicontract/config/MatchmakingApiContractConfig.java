package edu.rutmiit.pochaev.matchmakingapicontract.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * Центральное описание OpenAPI для Matchmaking API.
 *
 * Это не Spring-бин: springdoc подхватывает аннотации из contract-модуля,
 * когда основной сервис подключает этот jar как зависимость.
 */
@OpenAPIDefinition(
        info = @Info(
                title = "Matchmaking Arena API",
                version = "7.0",
                description = "REST API учебного сервиса матчмейкинга"
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local development")
        }
)
public final class MatchmakingApiContractConfig {

    private MatchmakingApiContractConfig() {
        // utility class
    }
}
