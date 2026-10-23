package com.fincore.server.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.JacksonException;

/** Returns stable problem responses without leaking SQL, credentials or provider bodies. */
@RestControllerAdvice
public class ApiExceptionHandler {
  /** Reports invalid request shapes, fields and cross-field semantics. */
  @ExceptionHandler({
    IllegalArgumentException.class,
    java.time.DateTimeException.class,
    ConstraintViolationException.class,
    JacksonException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentNotValidException.class
  })
  public ProblemDetail invalidRequest(Exception error) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "Invalid company data request. Check field types, required values and record references.");
  }

  /** Reports foreign-key, uniqueness, interval and immutable-record violations. */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail conflict(DataIntegrityViolationException error) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        "Record conflicts with an existing identity, reference or data-integrity rule.");
  }
}
