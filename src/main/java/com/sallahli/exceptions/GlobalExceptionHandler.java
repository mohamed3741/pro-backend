package com.sallahli.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorMessage> resourceNotFoundException(NotFoundException ex, WebRequest request) {
        ErrorMessage message = ErrorMessage.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error(ErrorType.GENERIC_ERROR.name())
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(message, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessage> resourceIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        ErrorMessage message = ErrorMessage.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(ErrorType.GENERIC_ERROR.name())
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorMessage> resourceBadRequestException(BadRequestException ex, WebRequest request) {
        ErrorMessage message = ErrorMessage.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(ex.getType().name())
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConflictAccountException.class)
    public ResponseEntity<ErrorMessage> resourceConflictAccountException(ConflictAccountException ex, WebRequest request) {
        ErrorMessage message = ErrorMessage.builder()
                .status(HttpStatus.CONFLICT.value())
                .error(ErrorType.GENERIC_ERROR.name())
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(message, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorMessage> accessDeniedException(AccessDeniedException ex, WebRequest request) {
        ErrorMessage message = ErrorMessage.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error(ErrorType.GENERIC_ERROR.name())
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();

        return new ResponseEntity<>(message, HttpStatus.FORBIDDEN);
    }
}
