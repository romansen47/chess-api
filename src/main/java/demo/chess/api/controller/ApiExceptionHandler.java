package demo.chess.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import demo.chess.api.dto.EngineUnavailableErrorDto;
import demo.chess.api.exception.NativeEngineUnavailableException;

/**
 * Central REST mapping for application-level API errors.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    public static final String ENGINE_UNAVAILABLE = "ENGINE_UNAVAILABLE";

    /**
     * Maps a missing native-engine requirement to a stable service-unavailable
     * response understood by API clients.
     *
     * @param exception missing-engine error
     * @return structured 503 response
     */
    @ExceptionHandler(NativeEngineUnavailableException.class)
    public ResponseEntity<EngineUnavailableErrorDto> handleNativeEngineUnavailable(
            NativeEngineUnavailableException exception) {
        EngineUnavailableErrorDto error = new EngineUnavailableErrorDto(
                ENGINE_UNAVAILABLE,
                exception.getRole(),
                exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error);
    }
}
