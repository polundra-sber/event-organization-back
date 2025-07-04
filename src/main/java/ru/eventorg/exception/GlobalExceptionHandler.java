package ru.eventorg.exception;

import org.openapitools.model.AuthUnsuccessResponse;
import org.openapitools.model.EventNotExistResponse;
import org.openapitools.model.UserAlreadyExistResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<UserAlreadyExistResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        UserAlreadyExistResponse response = new UserAlreadyExistResponse().error(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<AuthUnsuccessResponse> handleBadCredentialsException(BadCredentialsException ex) {
        AuthUnsuccessResponse response = new AuthUnsuccessResponse().error(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(EventNotExistException.class)
    public ResponseEntity<EventNotExistResponse> handleEventNotExistException(EventNotExistException ex) {
        EventNotExistResponse response = new EventNotExistResponse().error(ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(PurchaseNotExistException.class)
    public ResponseEntity<String> handlePurchaseNotExistException(PurchaseNotExistException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }

}

