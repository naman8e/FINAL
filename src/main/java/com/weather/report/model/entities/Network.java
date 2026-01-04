package com.weather.report.model.entities;

import java.util.Collection;
import java.util.ArrayList;

import com.weather.report.model.Timestamped;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.FetchType;
import jakarta.persistence.CascadeType;

/// A _monitoring network_ that represents a logical set of system elements.
/// 
/// It may have a list of _operators_ responsible for receiving notifications.
@Entity
public class Network extends Timestamped {

  @Id
  private String code;
  private String name;
  private String description;

  @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
  private Collection<Operator> operators = new ArrayList<>();

  // mappedby=network : the gateway entity has a field called network that owns
  // this relationship
  // the gateway table has the foreign key

  @OneToMany(mappedBy = "network", cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
  private Collection<Gateway> gateways = new ArrayList<>();

  public Network() {

  }

  public Network(String code, String name, String description) {
    this.code = code;
    this.name = name;
    this.description = description;
  }

  public Collection<Operator> getOperators() {
    return operators;
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

  public void setDescription(String description) {
    this.description = description;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public void setOperators(Collection<Operator> operators) {
    this.operators = operators;
  }

  public void addOperator(Operator operator) {
    this.operators.add(operator);
  }

  public void setName(String name) {
    this.name = name;
  }

  // gets collection of gateways connected to THIS PARTICULAR network
  public Collection<Gateway> getGateways() {
    if (gateways == null) {
      gateways = new ArrayList<>();
    }
    return gateways;
  }

  // sets the entire collection of gateways for THIS network
  public void setGateways(Collection<Gateway> gateways) {
    this.gateways = gateways;
  }

}
