package com.orbitastra.backend.common.error;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.orbitastra.backend.common.error.exception.ApiException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * Converts exceptions into the standard ApiError format.
 *
 * <p>Handles duplicate key errors and returns 409 instead of 500.
 */
@RestControllerAdvice // ! ← "check this class for handlers, on every controller" what this anonation does?
public class GlobalExceptionHandler {

        //? this will run due to DTO -> for all the misiing filed (For those are mark as @NotBlank, @Size(max = 30) anonation)
        // Step 2: ResponseEntity<ApiError>: throw the response
        // Error will throw in this data type ApiError
        //! who trigger this method: 1. @Valid
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> onValidation(MethodArgumentNotValidException exception) {
                Map<String, List<String>> fields = new LinkedHashMap<>();
                exception.getBindingResult().getFieldErrors().forEach(error -> fields
                                .computeIfAbsent(error.getField(), key -> new ArrayList<>())
                                .add(error.getDefaultMessage()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiError.createValidationError("One or more fields are invalid.", fields));
        }

        /**
         * Handles invalid request data, such as malformed JSON or wrong field types.
         * Returns a simple error instead of exposing internal application details.
         */
        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiError> onUnreadable(HttpMessageNotReadableException exception) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.createError("MALFORMED_REQUEST",
                                "The request body could not be read. Check that it is valid JSON and that every "
                                                + "field has the expected type."));
        }


        //! who trigger this method: anything that throws ApiException
        //! when the ApiException class/object created and thrown then @ExceptionHandler will call this method(onApiException) 
        //? one handler for 400 / 409 / 404 — the status rides on the exception itself,
        //? set by whichever ApiException factory raised it (badRequest / conflict / notFound)
        @ExceptionHandler(ApiException.class)
        public ResponseEntity<ApiError> onApiException(ApiException exception) {
                return ResponseEntity.status(exception.getStatus())
                                .body(ApiError.createError(exception.getCode(), exception.getMessage()));
        }

        @ExceptionHandler(DuplicateKeyException.class)
        public ResponseEntity<ApiError> onDuplicateKey(DuplicateKeyException exception) {
                // A unique index rejected the write. The application check that should have
                // caught it
                // lost a race, which is normal under concurrency and not a server fault.
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiError.createError("DUPLICATE_KEY",
                                                "That value is already in use. Another request may have just taken it."));
        }
}
