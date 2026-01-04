package com.weather.report.operations.topology;

import java.util.ArrayList;
import java.util.Collection;

import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.entities.Gateway;
import com.weather.report.model.entities.Sensor;
import com.weather.report.persistence.PersistenceManager;
import com.weather.report.utils.ValidationUtils;

import jakarta.persistence.EntityManager;

// this class will handle connecting , disconnecting of sensors to and from gateways... R4 Part B
public class SensorGatewayTopology {

    public SensorGatewayTopology() {
    }

    public Gateway connectSensor(String sensorCode, String gatewayCode, String username)
            throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException {

        // validating inputs
        if (sensorCode == null || sensorCode.trim().isEmpty()) {
            throw new InvalidInputDataException("Sensor code cannot be null or empty");
        }
        if (gatewayCode == null || gatewayCode.trim().isEmpty()) {
            throw new InvalidInputDataException("Gateway code cannot be null or empty");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new InvalidInputDataException("Username cannot be null or empty");
        }

        ValidationUtils.validateMaintainerUser(username);

        EntityManager em = PersistenceManager.getEntityManager();
        try {
            em.getTransaction().begin();

            // does gateway exist?
            Gateway gateway = em.find(Gateway.class, gatewayCode);
            if (gateway == null) {
                throw new ElementNotFoundException(
                        "Gateway with code '" + gatewayCode + "' not found");
            }

            // does sensor exist?
            Sensor sensor = em.find(Sensor.class, sensorCode);
            if (sensor == null) {
                throw new ElementNotFoundException(
                        "Sensor with code '" + sensorCode + "' not found");
            }

            if (gateway.getSensors() == null) {
                gateway.setSensors(new ArrayList<>());
            }

            boolean alreadyConnected = gateway.getSensors().stream()
                    .anyMatch(s -> s.getCode().equals(sensorCode));

            if (!alreadyConnected) {
                gateway.getSensors().add(sensor);
                sensor.setGateway(gateway);
            }

            em.getTransaction().commit();
            return gateway;

        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    public Gateway disconnectSensor(String sensorCode, String gatewayCode, String username)
            throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException {

        // validating inputs
        if (sensorCode == null || sensorCode.trim().isEmpty()) {
            throw new InvalidInputDataException("Sensor code cannot be null or empty");
        }
        if (gatewayCode == null || gatewayCode.trim().isEmpty()) {
            throw new InvalidInputDataException("Gateway code cannot be null or empty");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new InvalidInputDataException("Username cannot be null or empty");
        }

        ValidationUtils.validateMaintainerUser(username);

        EntityManager em = PersistenceManager.getEntityManager();
        try {
            em.getTransaction().begin();

            // does gateway exist?
            Gateway gateway = em.find(Gateway.class, gatewayCode);
            if (gateway == null) {
                throw new ElementNotFoundException(
                        "Gateway with code '" + gatewayCode + "' not found");
            }

            // does sensor exist?
            Sensor sensor = em.find(Sensor.class, sensorCode);
            if (sensor == null) {
                throw new ElementNotFoundException(
                        "Sensor with code '" + sensorCode + "' not found");
            }

            // is sensor connected to this gateway?
            if (gateway.getSensors() == null) {
                throw new ElementNotFoundException(
                        "Sensor '" + sensorCode + "' is not connected to gateway '" + gatewayCode + "'");
            }

            Sensor connectedSensor = gateway.getSensors().stream()
                    .filter(s -> s.getCode().equals(sensorCode))
                    .findFirst()
                    .orElse(null);

            if (connectedSensor == null) {
                throw new ElementNotFoundException(
                        "Sensor '" + sensorCode + "' is not connected to gateway '" + gatewayCode + "'");
            }

            // Remove the sensor from gateway's collection
            gateway.getSensors().remove(connectedSensor);

            // Clear the inverse relationship
            sensor.setGateway(null);

            em.getTransaction().commit();
            return gateway;

        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    public Collection<Sensor> getGatewaySensors(String gatewayCode)
            throws InvalidInputDataException, ElementNotFoundException {

        if (gatewayCode == null || gatewayCode.trim().isEmpty()) {
            throw new InvalidInputDataException("Gateway code cannot be null or empty");
        }

        EntityManager em = PersistenceManager.getEntityManager();
        try {
            Gateway gateway = em.find(Gateway.class, gatewayCode);
            if (gateway == null) {
                throw new ElementNotFoundException(
                        "Gateway with code '" + gatewayCode + "' not found");
            }

            if (gateway.getSensors() == null) {
                return new ArrayList<>();
            }

            return new ArrayList<>(gateway.getSensors());

        } finally {
            em.close();
        }
    }

}