package com.codergists.nio.files.exception;

public class SessionFileLockException extends RuntimeException {

  public SessionFileLockException() {
  }

  public SessionFileLockException(String message) {
    super(message);
  }

  public SessionFileLockException(String message, Throwable cause) {
    super(message, cause);
  }

  public SessionFileLockException(Throwable cause) {
    super(cause);
  }

  public SessionFileLockException(String message, Throwable cause, boolean enableSuppression,
                                  boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
