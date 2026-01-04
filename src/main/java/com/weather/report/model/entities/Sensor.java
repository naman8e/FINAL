package com.weather.report.model.entities;

import com.weather.report.model.Timestamped;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

/// A _sensor_ measures a physical quantity and periodically sends the
/// corresponding measurements.
/// 
/// A sensor may have a _threshold_ defined by the user to detect
/// anomalous behaviours.
@Entity
public class Sensor extends Timestamped {

  @Id
  private String code;
  private String name;
  private String description;

  // R3: One-to-One relationship with Threshold
  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  private Threshold threshold;

  // R4 Part B: Many-to-One relationship with Gateway
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "gateway_code")
  private Gateway gateway;

  public Sensor() {

  }

  public Sensor(String code, String name, String description) {
    this.code = code;
    this.name = name;
    this.description = description;
  }

  public Threshold getThreshold() {
    return threshold;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setThreshold(Threshold threshold) {
    this.threshold = threshold;
  }

  // R4 Part B: Getter and setter for gateway relationship
  public Gateway getGateway() {
    return gateway;
  }

  public void setGateway(Gateway gateway) {
    this.gateway = gateway;
  }
}
