package com.weather.report.model.entities;

import com.weather.report.model.ThresholdType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/// A _threshold_ defines an acceptable limit for the values measured by a
/// sensor.
///
/// It always consists of a numeric value and a ThresholdType that the system
/// must apply to decide whether a measurement is anomalous.
@Entity
@Table(name = "THRESHOLD")
public class Threshold {

  @Id
  @Column(name = "SENSOR_CODE", nullable = false, updatable = false, length = 8)
  private String sensorCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "TYPE", nullable = false)
  private ThresholdType type;

  @Column(name = "THRESHOLD_VALUE", nullable = false)
  private double value;

  protected Threshold() {
    // JPA only
  }

  public Threshold(String sensorCode, ThresholdType type, double value) {
    this.sensorCode = Objects.requireNonNull(sensorCode);
    this.type = Objects.requireNonNull(type);
    this.value = value;
  }

  public String getSensorCode() {
    return sensorCode;
  }

  public ThresholdType getType() {
    return type;
  }

  public double getValue() {
    return value;
  }

  public void setType(ThresholdType type) {
    this.type = Objects.requireNonNull(type);
  }

  public void setValue(double value) {
    this.value = value;
  }
}
