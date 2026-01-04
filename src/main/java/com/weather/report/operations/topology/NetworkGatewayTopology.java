package com.weather.report.operations.topology;

import com.weather.report.exceptions.*;
import com.weather.report.model.entities.Gateway;
import com.weather.report.model.entities.Network;
import com.weather.report.persistence.PersistenceManager;
import com.weather.report.utils.ValidationUtils;

import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.Collection;

//this class will handle connecting , disconnecting  of Agateways to and from networks... R4
public class NetworkGatewayTopology {

    public NetworkGatewayTopology() {
    }

    public Network connectGateway(String networkCode, String gatewayCode, String username)
            throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException {

        // validating inputs
        ValidationUtils.validateNetworkCode(networkCode);
        ValidationUtils.validateGatewayCode(gatewayCode);
        ValidationUtils.validateMaintainerUser(username);

        EntityManager em = PersistenceManager.getEntityManager();
        try {
            em.getTransaction().begin();
            // does network exist?
            Network network = em.find(Network.class, networkCode);
            if (network == null) {
                throw new ElementNotFoundException("Network with code '" + networkCode + "' not found");
            }
            // does gateway exist?
            Gateway gateway = em.find(Gateway.class, gatewayCode);
            if (gateway == null) {
                throw new ElementNotFoundException("Gateway with code '" + gatewayCode + "' not found");
            }
            // is gateway connected to this network?
            if (network.getGateways() == null) {
                network.setGateways(new ArrayList<>());
            }

            boolean alreadyConnected = network.getGateways().stream()
                    .anyMatch(g -> g.getCode().equals(gatewayCode));

            if (!alreadyConnected) {
                network.getGateways().add(gateway);
                gateway.setNetwork(network);
            }

            em.getTransaction().commit();
            return network;

        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    public Network disconnectGateway(String networkCode, String gatewayCode, String username)
            throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException {

        // Validate inputs
        ValidationUtils.validateNetworkCode(networkCode);
        ValidationUtils.validateGatewayCode(gatewayCode);
        ValidationUtils.validateMaintainerUser(username);

        EntityManager em = PersistenceManager.getEntityManager();
        try {
            em.getTransaction().begin();

            // does network exist?
            Network network = em.find(Network.class, networkCode);
            if (network == null) {
                throw new ElementNotFoundException("Network with code '" + networkCode + "' not found");
            }

            // gateway exists?
            Gateway gateway = em.find(Gateway.class, gatewayCode);
            if (gateway == null) {
                throw new ElementNotFoundException("Gateway with code '" + gatewayCode + "' not found");
            }

            // Check if gateway is connected to this network
            if (network.getGateways() == null) {
                throw new ElementNotFoundException(
                        "Gateway '" + gatewayCode + "' is not connected to network '" + networkCode + "'");
            }

            Gateway connectedGateway = network.getGateways().stream()
                    .filter(g -> g.getCode().equals(gatewayCode))
                    .findFirst()
                    .orElse(null);

            if (connectedGateway == null) {
                throw new ElementNotFoundException(
                        "Gateway '" + gatewayCode + "' is not connected to network '" + networkCode + "'");
            }
            network.getGateways().remove(connectedGateway);
            gateway.setNetwork(null);

            em.getTransaction().commit();
            return network;

        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();

        }
    }

    public Collection<Gateway> getNetworkGateways(String networkCode)
            throws InvalidInputDataException, ElementNotFoundException {

        ValidationUtils.validateNetworkCode(networkCode);

        EntityManager em = PersistenceManager.getEntityManager();
        try {
            Network network = em.find(Network.class, networkCode);
            if (network == null) {
                throw new ElementNotFoundException("Network with code '" + networkCode + "' not found");
            }
            if (network.getGateways() == null) {
                return new ArrayList<>();
            }

            return new ArrayList<>(network.getGateways());

        } finally {
            em.close();
        }
    }

}