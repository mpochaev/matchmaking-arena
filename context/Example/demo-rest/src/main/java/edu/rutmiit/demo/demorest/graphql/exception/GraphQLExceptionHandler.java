package edu.rutmiit.demo.demorest.graphql.exception;

import com.netflix.graphql.types.errors.TypedGraphQLError;
import edu.rutmiit.demo.booksapicontract.exception.IsbnAlreadyExistsException;
import edu.rutmiit.demo.booksapicontract.exception.ResourceNotFoundException;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Обработчик исключений для GraphQL DataFetcher'ов.
 *
 * В REST API исключения преобразуются в HTTP-статусы (404, 409, 400).
 * В GraphQL нет HTTP-статусов для ошибок — все ответы приходят с HTTP 200,
 * а ошибки помещаются в массив "errors" в теле ответа.
 *
 * Этот обработчик перехватывает доменные исключения и преобразует их
 * в типизированные GraphQL-ошибки с понятными сообщениями и классификацией.
 *
 * DGS автоматически обнаруживает реализацию DataFetcherExceptionHandler
 * как Spring-компонент и подставляет её вместо обработчика по умолчанию.
 */
@Component
public class GraphQLExceptionHandler implements DataFetcherExceptionHandler {

    @Override
    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(
            DataFetcherExceptionHandlerParameters handlerParameters) {

        Throwable exception = handlerParameters.getException();

        // Ресурс не найден — аналог HTTP 404
        if (exception instanceof ResourceNotFoundException) {
            var error = TypedGraphQLError.newNotFoundBuilder()
                    .message(exception.getMessage())
                    .path(handlerParameters.getPath())
                    .build();

            return CompletableFuture.completedFuture(
                    DataFetcherExceptionHandlerResult.newResult()
                            .error(error)
                            .build());
        }

        // Конфликт ISBN — аналог HTTP 409
        if (exception instanceof IsbnAlreadyExistsException) {
            var error = TypedGraphQLError.newConflictBuilder()
                    .message(exception.getMessage())
                    .path(handlerParameters.getPath())
                    .build();

            return CompletableFuture.completedFuture(
                    DataFetcherExceptionHandlerResult.newResult()
                            .error(error)
                            .build());
        }

        // Все остальные исключения — внутренняя ошибка сервера.
        // Не раскрываем детали клиенту в целях безопасности.
        var error = TypedGraphQLError.newInternalErrorBuilder()
                .message("Внутренняя ошибка сервера")
                .path(handlerParameters.getPath())
                .build();

        return CompletableFuture.completedFuture(
                DataFetcherExceptionHandlerResult.newResult()
                        .error(error)
                        .build());
    }
}
