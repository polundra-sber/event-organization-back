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
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<AuthUnsuccessResponse> handleBadCredentialsException(BadCredentialsException ex) {
        AuthUnsuccessResponse response = new AuthUnsuccessResponse().error(ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(EventNotExistException.class)
    public ResponseEntity<EventNotExistResponse> handleEventNotExistException(EventNotExistException ex) {
        EventNotExistResponse response = new EventNotExistResponse().error(ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(UserAlreadyJoinException.class)
    public ResponseEntity<String> handleUserAlreadyJoinException(UserAlreadyJoinException ex) {
        String response = ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(PurchaseNotExistException.class)
    public ResponseEntity<String> handlePurchaseNotExistException(PurchaseNotExistException ex) {
        String response = ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(StuffNotExistException.class)
    public ResponseEntity<String> handleStuffNotExistException(StuffNotExistException ex) {
        String response = ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(UserNotEventParticipantException.class)
    public ResponseEntity<String> handleUserNotEventParticipantException(UserNotEventParticipantException ex) {
        String response = ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(WrongUserRoleException.class)
    public ResponseEntity<String> handleWrongUserRoleException(WrongUserRoleException ex) {
        String response = ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<String> handleTaskNotFoundException(TaskNotFoundException ex) {
        String response = ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    public ResponseEntity<String> handleUserAlreadyParticipantException(UserAlreadyParticipantException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }

    @ExceptionHandler(EventNotActiveException.class)
    public ResponseEntity<String> handleEventNotActiveException(EventNotActiveException ex) {
        String response = ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(AlreadyHasResponsibleException.class)
    public ResponseEntity<String> handleAlreadyHasResponsibleException(AlreadyHasResponsibleException ex) {
        String response = ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<String> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        String response = ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(NotResponsibleException.class)
    public ResponseEntity<String> handleNotResponsibleException(NotResponsibleException ex) {
        String response = ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(TaskAlreadyCompletedException.class)
    public ResponseEntity<String> handleTaskAlreadyCompletedException(TaskAlreadyCompletedException ex) {
        String response = ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(DebtNotExistsException.class)
    public ResponseEntity<String> handleDebtNotExistsException(DebtNotExistsException ex) {
        String response = ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(UserNotRecipientException.class)
    public ResponseEntity<String> handleUserNotRecipientException(UserNotRecipientException ex) {
        String response = ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(UserNotPayerException.class)
    public ResponseEntity<String> handleUserNotPayerException(UserNotPayerException ex) {
        String response = ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(RoleOfCreatorIsUnchangeable.class)
    public ResponseEntity<String> handleRoleOfCreatorIsUnchangeable(RoleOfCreatorIsUnchangeable ex) {
        String response = ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }
}

