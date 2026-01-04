package com.weather.report.model.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/// Represnts a measurement taken by a sensor in the weather report system
@Entity
public class Measurement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String sensorCode;
  private String gatewayCode;
  private String networkCode;

  @Column(name = "measurement_value")
  private double value;

  @Column(name = "measurement_timestamp")
  private LocalDateTime timestamp;

  public Measurement() { 
    // JPA Compliance
  }

  public Measurement(String networkCode, String gatewayCode, String sensorCode, double value, LocalDateTime timestamp) {
    this.networkCode = networkCode;
    this.gatewayCode = gatewayCode;
    this.sensorCode = sensorCode;
    this.value = value;
    this.timestamp = timestamp;
  }

  /// Id of the measurement
  public Long getId() {
    return this.id;
  }

  /// Code of the network to which the gateway is connected
  public String getNetworkCode() {
    return this.networkCode;
  }

  /// Code of the gateway the sensor that collected the measure is part of
  public String getGatewayCode() {
    return this.gatewayCode;
  }

  /// Code of the sensor that performed the measurement
  public String getSensorCode() {
    return this.sensorCode;
  }

  /// The measurement value
  public double getValue() {
    return this.value;
  }

  /// The timestamp of the measurement
  public LocalDateTime getTimestamp() {
    return this.timestamp;
  }
}
