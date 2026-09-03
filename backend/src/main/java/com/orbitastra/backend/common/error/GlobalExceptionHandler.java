package com.orbitastra.backend.common.error;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.exc.InvalidFormatException;
import com.orbitastra.backend.common.error.exception.ApiException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


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
                // A misspelled enum arrives here rather than as a type mismatch, because it
                // failed inside the body rather than on a query parameter. Left generic, the
                // caller is told the body "could not be read" — true, and useless against a
                // twenty-value enum they now have to guess at.
                //
                // NOTE the import: Boot 4.1 ships Jackson 3, whose root package is
                // tools.jackson, not com.fasterxml.jackson. The old package is still on the
                // classpath for jackson-annotations, so an IDE offers the wrong one first and
                // it compiles nowhere. Jackson 3 also renamed Reference.getFieldName() to
                // getPropertyName(), which is why the path walk below reads differently from
                // every Jackson 2 example.
                if (exception.getCause() instanceof InvalidFormatException invalid
                                && invalid.getTargetType() != null
                                && invalid.getTargetType().isEnum()) {

                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(ApiError.createError("INVALID_VALUE",
                                                        "'" + invalid.getValue() + "' is not a valid value for '"
                                                                        + fieldPath(invalid) + "'. Accepted values: "
                                                                        + String.join(", ", Arrays
                                                                                        .stream(invalid.getTargetType()
                                                                                                        .getEnumConstants())
                                                                                        .map(Object::toString).toList())
                                                                        + "."));
                }

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.createError("MALFORMED_REQUEST",
                                "The request body could not be read. Check that it is valid JSON and that every "
                                                + "field has the expected type."));
        }

        /**
         * Where in the body the bad value was, as {@code features[0].featureCode}.
         *
         * <p>Jackson records the path it was walking when it gave up. Rebuilding it matters most
         * exactly where it is hardest to work out by eye: one wrong value in a list of twenty.
         */
        private String fieldPath(InvalidFormatException invalid) {
                StringBuilder path = new StringBuilder();
                for (tools.jackson.core.JacksonException.Reference step : invalid.getPath()) {
                        if (step.getPropertyName() != null) {
                                if (path.length() > 0) {
                                        path.append('.');
                                }
                                path.append(step.getPropertyName());
                        } else if (step.getIndex() >= 0) {
                                path.append('[').append(step.getIndex()).append(']');
                        }
                }
                return path.length() == 0 ? "the request body" : path.toString();
        }


        /**
         * A required query parameter was left out.
         *
         * <p>Without this, Spring answers with its own error page — which in dev carries a full
         * stack trace, and in any environment does not look like the rest of our errors. The
         * caller gets the same shape as everything else here, and the parameter's name.
         *
         * <p>The one place this matters most is the bulk holiday delete, where {@code type} is
         * required precisely because forgetting it must not clear a whole calendar.
         */
        @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<ApiError> onMissingParameter(MissingServletRequestParameterException exception) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiError.createError("MISSING_PARAMETER",
                                                "The '" + exception.getParameterName()
                                                                + "' query parameter is required."));
        }

        /**
         * A path variable or query parameter could not be turned into the type it needs to be:
         * a misspelled enum such as {@code ?type=WEEKLYOFF}, or a date like
         * {@code /holidays/08-11-2026} that is not ISO.
         *
         * <p>Where the target is an enum the message lists what is accepted, because "expected
         * type HolidayType" tells the caller nothing they can act on.
         */
        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiError> onTypeMismatch(MethodArgumentTypeMismatchException exception) {
                Class<?> wanted = enumBehind(exception);
                String allowed = wanted != null
                                ? " Accepted values: " + String.join(", ",
                                                java.util.Arrays.stream(wanted.getEnumConstants())
                                                                .map(Object::toString).toList()) + "."
                                : "";

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiError.createError("INVALID_PARAMETER",
                                                "'" + exception.getValue() + "' is not a valid value for '"
                                                                + exception.getName() + "'." + allowed));
        }


        /**
         * The enum a parameter wanted, whether it wanted one or a list of them.
         *
         * <p>A repeatable parameter such as {@code ?status=ACTIVE&status=TRIAL} binds to
         * {@code List<SchoolStatus>}, so the required type is {@code List} and the enum is only
         * visible as its element type. Without this, the most useful half of the message — the
         * values that would have worked — silently disappears for exactly the parameters most
         * likely to be misspelled.
         *
         * <p>Returns null when nothing enum-shaped is involved.
         */
        private Class<?> enumBehind(MethodArgumentTypeMismatchException exception) {
                Class<?> required = exception.getRequiredType();
                if (required != null && required.isEnum()) {
                        return required;
                }
                if (exception.getParameter() == null) {
                        return null;
                }
                Class<?> element = exception.getParameter().nested().getNestedParameterType();
                return element != null && element.isEnum() ? element : null;
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
