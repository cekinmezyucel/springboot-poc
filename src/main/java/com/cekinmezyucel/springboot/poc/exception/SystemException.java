package com.cekinmezyucel.springboot.poc.exception;

public class SystemException extends RuntimeException {
  public SystemException(String message) {
    super(message);
  }
}
