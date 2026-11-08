package com.govener.adapter.in.web;

import com.govener.shared.domain.RuleViolation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public final class ApiExceptionAdvice {
  @ExceptionHandler(RuleViolation.class)
  ResponseEntity<?> rule(RuleViolation failure, HttpServletRequest request) {
    int status = ProblemResponses.status(failure);
    return response(status, failure.code(), failure.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> validation(
      MethodArgumentNotValidException failure, HttpServletRequest request) {
    var body =
        ProblemResponses.body(
            422, "VALIDATION_FAILED", "One or more fields are invalid.", request.getRequestURI());
    body.put(
        "fieldErrors",
        failure.getBindingResult().getFieldErrors().stream()
            .map(
                error ->
                    Map.of(
                        "field",
                        error.getField(),
                        "code",
                        "INVALID_FIELD",
                        "message",
                        error.getDefaultMessage() == null
                            ? "Invalid value."
                            : error.getDefaultMessage()))
            .toList());
    return ResponseEntity.unprocessableEntity()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(body);
  }

  @ExceptionHandler({
    ConstraintViolationException.class,
    org.springframework.web.method.annotation.HandlerMethodValidationException.class
  })
  ResponseEntity<?> constraint(Exception failure, HttpServletRequest request) {
    return response(422, "VALIDATION_FAILED", "One or more parameters are invalid.", request);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class,
    org.springframework.web.bind.MissingRequestHeaderException.class,
    org.springframework.web.multipart.support.MissingServletRequestPartException.class
  })
  ResponseEntity<?> malformed(Exception failure, HttpServletRequest request) {
    return response(400, "INVALID_REQUEST", "The request format is invalid.", request);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<?> upload(Exception failure, HttpServletRequest request) {
    return response(413, "UPLOAD_TOO_LARGE", "The attachment exceeds the permitted size.", request);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<?> missing(Exception failure, HttpServletRequest request) {
    return response(404, "RESOURCE_NOT_FOUND", "Resource is unavailable.", request);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> unexpected(Exception failure, HttpServletRequest request) {
    org.slf4j.LoggerFactory.getLogger(getClass())
        .error(
            "Unexpected API failure at {}: {}",
            request.getRequestURI(),
            failure.getClass().getSimpleName());
    return response(500, "INTERNAL_ERROR", "The operation could not be completed.", request);
  }

  private ResponseEntity<?> response(
      int status, String code, String detail, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(ProblemResponses.body(status, code, detail, request.getRequestURI()));
  }
}
