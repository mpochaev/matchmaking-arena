package edu.rutmiit.pochaev.exception;

import edu.rutmiit.pochaev.matchmakingapicontract.dto.ErrorResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.exception.InvalidLobbyOperationException;
import edu.rutmiit.pochaev.matchmakingapicontract.exception.LobbyHasPlayersException;
import edu.rutmiit.pochaev.matchmakingapicontract.exception.PlayerAlreadyInLobbyException;
import edu.rutmiit.pochaev.matchmakingapicontract.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_PROBLEM_URI = "https://api.matchmaking-arena.local/problems/";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND,
                "resource-not-found",
                "Ресурс не найден",
                ex.getMessage(),
                request.getRequestURI(),
                null);
    }

    @ExceptionHandler({PlayerAlreadyInLobbyException.class, InvalidLobbyOperationException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT,
                "conflict",
                "Конфликт операции",
                ex.getMessage(),
                request.getRequestURI(),
                null);
    }

    @ExceptionHandler(LobbyHasPlayersException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(LobbyHasPlayersException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN,
                "forbidden",
                "Операция запрещена",
                ex.getMessage(),
                request.getRequestURI(),
                null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(
                        error.getField(),
                        error.getRejectedValue(),
                        error.getDefaultMessage()
                ))
                .toList();

        String detail = fieldErrors.stream()
                .map(error -> error.field() + ": " + error.message())
                .reduce((left, right) -> left + "; " + right)
                .orElse("Ошибка валидации входных данных");

        return build(HttpStatus.BAD_REQUEST,
                "validation-error",
                "Ошибка валидации",
                detail,
                request.getRequestURI(),
                fieldErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String detail = "Некорректное значение параметра " + ex.getName() + ": " + ex.getValue();
        List<ErrorResponse.FieldError> fieldErrors = List.of(new ErrorResponse.FieldError(
                ex.getName(),
                ex.getValue(),
                "Некорректное значение"
        ));
        return build(HttpStatus.BAD_REQUEST,
                "validation-error",
                "Ошибка валидации",
                detail,
                request.getRequestURI(),
                fieldErrors);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "validation-error",
                "Ошибка валидации",
                ex.getMessage(),
                request.getRequestURI(),
                null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-error",
                "Внутренняя ошибка сервера",
                "Произошла непредвиденная ошибка. Обратитесь к поддержке.",
                request.getRequestURI(),
                null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status,
                                                String type,
                                                String title,
                                                String detail,
                                                String instance,
                                                List<ErrorResponse.FieldError> fieldErrors) {
        ErrorResponse body = new ErrorResponse(
                status.value(),
                BASE_PROBLEM_URI + type,
                title,
                detail,
                instance,
                Instant.now(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
