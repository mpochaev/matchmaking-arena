package com.example.demo.graphql.exception;

import com.netflix.graphql.types.errors.TypedGraphQLError;
import edu.rutmiit.demo.matchmakingapicontract.exception.InvalidLobbyOperationException;
import edu.rutmiit.demo.matchmakingapicontract.exception.LobbyHasPlayersException;
import edu.rutmiit.demo.matchmakingapicontract.exception.PlayerAlreadyInLobbyException;
import edu.rutmiit.demo.matchmakingapicontract.exception.ResourceNotFoundException;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Обработка ошибок GraphQL.
 * В GraphQL ошибка возвращается в массиве errors, а не через HTTP 404/409.
 */
@Component
public class GraphQLExceptionHandler implements DataFetcherExceptionHandler {

    @Override
    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(
            DataFetcherExceptionHandlerParameters handlerParameters) {

        Throwable exception = handlerParameters.getException();

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

        if (exception instanceof PlayerAlreadyInLobbyException
                || exception instanceof InvalidLobbyOperationException) {
            var error = TypedGraphQLError.newConflictBuilder()
                    .message(exception.getMessage())
                    .path(handlerParameters.getPath())
                    .build();

            return CompletableFuture.completedFuture(
                    DataFetcherExceptionHandlerResult.newResult()
                            .error(error)
                            .build());
        }

        if (exception instanceof LobbyHasPlayersException) {
            var error = TypedGraphQLError.newPermissionDeniedBuilder()
                    .message(exception.getMessage())
                    .path(handlerParameters.getPath())
                    .build();

            return CompletableFuture.completedFuture(
                    DataFetcherExceptionHandlerResult.newResult()
                            .error(error)
                            .build());
        }

        var error = TypedGraphQLError.newInternalErrorBuilder()
                .message("Internal server error")
                .path(handlerParameters.getPath())
                .build();

        return CompletableFuture.completedFuture(
                DataFetcherExceptionHandlerResult.newResult()
                        .error(error)
                        .build());
    }
}
