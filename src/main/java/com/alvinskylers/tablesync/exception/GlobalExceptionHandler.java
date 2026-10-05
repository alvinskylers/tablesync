package com.alvinskylers.tablesync.exception;

import com.alvinskylers.tablesync.dto.error.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validationHandler(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldErrorDetail> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe ->  new ErrorResponse.FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("VALIDATION_ERROR", "One field or more is invalid.",details));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> authorizationDeniedExceptionHandler(AuthorizationDeniedException ex){

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("FORBIDDEN", "You do not have the permissions to perform this action", null));
    }

     @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("MALFORMED_REQUEST_BODY", "Request body is missing or invalid", null));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> emailTakenHandler(EmailAlreadyExistsException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("EMAIL_TAKEN", ex.getMessage(), null));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> badCredentialsExceptionHandler(BadCredentialsException ex) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("INCORRECT_CREDENTIALS",  "Invalid email or password", null));
    }

    @ExceptionHandler(TableNotFoundException.class)
    public ResponseEntity<ErrorResponse> tableNotFoundExceptionHandler(TableNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("TABLE_NOT_FOUND", ex.getMessage(), null));
    }

    @ExceptionHandler(TableNumberExistsException.class)
    public ResponseEntity<ErrorResponse> tableNumberExistsExceptionHandler(TableNumberExistsException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("TABLE_NUMBER_TAKEN", ex.getMessage(), null));
    }

    @ExceptionHandler(InvalidReservationTimeException.class)
    public ResponseEntity<ErrorResponse> invalidReservationTimeExceptionHandler(InvalidReservationTimeException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("INVALID_RESERVATION_TIME", ex.getMessage(), null));
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ErrorResponse> reservationNotFoundExceptionHandler(ReservationNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("RESERVATION_NOT_FOUND", ex.getMessage(), null));
    }

    @ExceptionHandler(TableReservedException.class)
    public ResponseEntity<ErrorResponse> tableReservationNotFoundExceptionHandler(TableReservedException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("TABLE_RESERVED", ex.getMessage(), null));
    }

    @ExceptionHandler(ReservationUpdateStatusException.class)
    public ResponseEntity<ErrorResponse>reservationUpdateStatusExceptionHandler(ReservationUpdateStatusException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("INVALID_RESERVATION_TRANSITION_STATUS", ex.getMessage(), null));
    }

}
