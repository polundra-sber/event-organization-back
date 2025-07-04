package ru.eventorg.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserNotEventParticipantException extends RuntimeException {

  private final HttpStatus statusCode;
  private final String message;

  public UserNotEventParticipantException(ErrorState error) {
    this.statusCode = error.getStatusCode();
    this.message = error.getMessage();
  }
}