package com.weather.report.model.entities;

import java.util.Collection;
import java.util.ArrayList;
import com.weather.report.model.Timestamped;
import jakarta.persistence.*;

@Entity
@Table(name = "gateways")
public class Gateway extends Timestamped {

    @Id
    @Column(name = "code", length = 7)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "gateway", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Collection<Parameter> parameters = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_code")
    private Network network;

    // R4 Part B: One-to-Many relationship with Sensor
    @OneToMany(mappedBy = "gateway", cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    private Collection<Sensor> sensors = new ArrayList<>();

    public Gateway() {
    }

    public Gateway(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public Collection<Parameter> getParameters() {
        return parameters;
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

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    // R4 Part B: getter and setter for sensor relationship
    public Collection<Sensor> getSensors() {
        if (sensors == null) {
            sensors = new ArrayList<>();
        }
        return sensors;
    }

    public void setSensors(Collection<Sensor> sensors) {
        this.sensors = sensors;
    }
}