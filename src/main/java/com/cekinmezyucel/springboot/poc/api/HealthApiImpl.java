package com.cekinmezyucel.springboot.poc.api;

import java.util.List;

import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class HealthApiImpl implements HealthApiDelegate {
  private final List<HealthIndicator> healthIndicators;

  public HealthApiImpl(List<HealthIndicator> healthIndicators) {
    this.healthIndicators = healthIndicators;
  }

  @Override
  public ResponseEntity<Void> getHealth() {
    boolean allUp =
        healthIndicators.stream()
            .map(HealthIndicator::health)
            .map(health -> health != null ? health.getStatus() : Status.DOWN)
            .allMatch(Status.UP::equals);

    return allUp
        ? ResponseEntity.status(HttpStatus.OK).build()
        : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
  }
}
