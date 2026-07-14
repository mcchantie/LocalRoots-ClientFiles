package com.localroots.clientfiles.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException exception, HttpServletRequest request) {
        if (exception.getStatus().is5xxServerError()) {
            log.error(
                    "API request failed method={} path={} status={} title={} detail={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    exception.getStatus().value(),
                    exception.getTitle(),
                    exception.getMessage(),
                    exception
            );
        } else {
            log.warn(
                    "API request rejected method={} path={} status={} title={} detail={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    exception.getStatus().value(),
                    exception.getTitle(),
                    exception.getMessage()
            );
        }

        ProblemDetail problem = problem(exception.getStatus(), exception.getTitle(), exception.getMessage(), request);
        return ResponseEntity.status(exception.getStatus()).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleInvalidBody(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more request fields are invalid.",
                request
        );
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        problem.setProperty("errors", errors);
        log.warn(
                "Request body validation failed method={} path={} fields={}",
                request.getMethod(),
                request.getRequestURI(),
                errors
        );
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        Throwable cause = exception.getMostSpecificCause();
        log.warn(
                "Request body could not be read method={} path={} causeType={}",
                request.getMethod(),
                request.getRequestURI(),
                cause == null ? exception.getClass().getSimpleName() : cause.getClass().getSimpleName()
        );
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request body",
                "The JSON body is malformed or contains an unsupported value.",
                request
        );
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Request parameter type mismatch method={} path={} parameter={} requiredType={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getName(),
                exception.getRequiredType() == null ? "unknown" : exception.getRequiredType().getSimpleName()
        );
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request parameter",
                "The value supplied for " + exception.getName() + " is invalid.",
                request
        );
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            Exception exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Request validation failed method={} path={} exceptionType={} detail={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName(),
                exception.getMessage()
        );
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                exception.getMessage(),
                request
        );
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "HTTP method is not supported method={} path={} supportedMethods={}",
                exception.getMethod(),
                request.getRequestURI(),
                exception.getSupportedHttpMethods()
        );
        ProblemDetail problem = problem(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method not allowed",
                "The " + exception.getMethod() + " method is not supported for this endpoint.",
                request
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(problem);
    }


    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "API endpoint was not found method={} path={}",
                request.getMethod(),
                request.getRequestURI()
        );
        ProblemDetail problem = problem(
                HttpStatus.NOT_FOUND,
                "Endpoint not found",
                "No API endpoint matches this request.",
                request
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<ProblemDetail> handleDatabaseFailure(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Database request failed method={} path={} exceptionType={} rootCause={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getName(),
                rootCauseType(exception),
                exception
        );
        ProblemDetail problem = problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Database request failed",
                "The database could not complete the request. Retry the operation.",
                request
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    @ExceptionHandler(S3Exception.class)
    public ResponseEntity<ProblemDetail> handleS3Exception(S3Exception exception, HttpServletRequest request) {
        String awsMessage = exception.awsErrorDetails() == null
                ? exception.getMessage()
                : exception.awsErrorDetails().errorMessage();
        String errorCode = exception.awsErrorDetails() == null
                ? "unknown"
                : exception.awsErrorDetails().errorCode();

        log.error(
                "S3 request failed method={} path={} awsStatus={} awsErrorCode={} awsRequestId={} awsExtendedRequestId={} awsMessage={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.statusCode(),
                errorCode,
                exception.requestId(),
                exception.extendedRequestId(),
                awsMessage,
                exception
        );

        ProblemDetail problem = problem(
                HttpStatus.BAD_GATEWAY,
                "S3 request failed",
                awsMessage == null ? "Amazon S3 could not complete the request." : awsMessage,
                request
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error(
                "Unexpected request failure method={} path={} exceptionType={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getName(),
                exception
        );
        ProblemDetail problem = problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected server error",
                "The request could not be completed.",
                request
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }


    private String rootCauseType(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getClass().getName();
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

        String correlationId = MDC.get(LoggingContext.CORRELATION_ID);
        if (correlationId != null && !correlationId.isBlank()) {
            problem.setProperty("correlationId", correlationId);
        }

        return problem;
    }
}
