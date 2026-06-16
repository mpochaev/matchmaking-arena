package edu.rutmiit.pochaev.matchmakingapicontract.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String type,
        String title,
        String detail,
        String instance,
        Instant timestamp,
        List<FieldError> fieldErrors
) {
    public record FieldError(
            String field,
            Object rejectedValue,
            String message
    ) {}
}
