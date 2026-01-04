package com.weather.report.operations;

import com.weather.report.exceptions.*;
import com.weather.report.model.entities.Gateway;
import com.weather.report.model.entities.Network;
import com.weather.report.model.entities.Sensor;
import com.weather.report.operations.topology.NetworkGatewayTopology;
import com.weather.report.operations.topology.SensorGatewayTopology;

import java.util.Collection;

// Delegates topology operations
public class TopologyOperationsImpl implements TopologyOperations {

    private final NetworkGatewayTopology networkGatewayTopology;
    private final SensorGatewayTopology gatewaySensorTopology;

    // Init delegates
    public TopologyOperationsImpl() {
        this.networkGatewayTopology = new NetworkGatewayTopology();
        this.gatewaySensorTopology = new SensorGatewayTopology();
    }

    // Network → Gateways
    @Override
    public Collection<Gateway> getNetworkGateways(String networkCode)
            throws InvalidInputDataException, ElementNotFoundException {
        return networkGatewayTopology.getNetworkGateways(networkCode);
    }

    // Link gateway to network
    @Override
    public Network connectGateway(String networkCode, String gatewayCode, String username)
            throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException {
        return networkGatewayTopology.connectGateway(networkCode, gatewayCode, username);
    }

    // Unlink gateway from network
    @Override
    public Network disconnectGateway(String networkCode, String gatewayCode, String username)
            throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException {
        return networkGatewayTopology.disconnectGateway(networkCode, gatewayCode, username);
    }

    // Gateway → Sensors
    @Override
    public Collection<Sensor> getGatewaySensors(String gatewayCode)
            throws InvalidInputDataException, ElementNotFoundException {
        return gatewaySensorTopology.getGatewaySensors(gatewayCode);
    }

    // Link sensor to gateway
    @Override
    public Gateway connectSensor(String sensorCode, String gatewayCode, String username)
            throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException {
        return gatewaySensorTopology.connectSensor(sensorCode, gatewayCode, username);
    }

    // Unlink sensor from gateway
    @Override
    public Gateway disconnectSensor(String sensorCode, String gatewayCode, String username)
            throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException {
        return gatewaySensorTopology.disconnectSensor(sensorCode, gatewayCode, username);
    }
}