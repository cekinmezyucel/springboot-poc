package com.cekinmezyucel.springboot.poc.api;

import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthApiImpl implements HealthApi {
  private final HealthEndpoint healthEndpoint;

  public HealthApiImpl(HealthEndpoint healthEndpoint) {
    this.healthEndpoint = healthEndpoint;
  }

  @Override
  public ResponseEntity<Void> getHealth() {
    Status status = healthEndpoint.health().getStatus();
    if (Status.UP.equals(status)) {
      return ResponseEntity.status(HttpStatus.OK).build();
    } else {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
