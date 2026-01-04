package com.weather.report.model.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "parameters")
public class Parameter {

    public static final String EXPECTED_MEAN_CODE = "EXPECTED_MEAN";
    public static final String EXPECTED_STD_DEV_CODE = "EXPECTED_STD_DEV";
    public static final String BATTERY_CHARGE_PERCENTAGE_CODE = "BATTERY_CHARGE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "numeric_value")
    private Double value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gateway_code")
    private Gateway gateway;

    public Parameter() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Gateway getGateway() {
        return gateway;
    }

    public void setGateway(Gateway gateway) {
        this.gateway = gateway;
    }
}