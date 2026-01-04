package com.weather.report.operations;

import java.util.Collection;

import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.entities.Gateway;
import com.weather.report.model.entities.Network;
import com.weather.report.model.entities.Sensor;

/**
 * Operations for managing topology relationships between networks, gateways and
 * sensors (R4).
 * Implementations must validate inputs, respect permissions and
 * handle missing elements according to the exception model.
 */
public interface TopologyOperations {

  /**
   * Returns gateways associated with a network.
   *
   * @param networkCode network code (mandatory)
   * @return collection of gateways linked to the network
   * @throws InvalidInputDataException when mandatory data are invalid
   * @throws ElementNotFoundException  when the network does not exist
   */
  public Collection<Gateway> getNetworkGateways(String networkCode)
      throws InvalidInputDataException, ElementNotFoundException;

  /**
   * Associates a gateway to a network.
   *
   * @param networkCode network code (mandatory)
   * @param gatewayCode gateway code (mandatory)
   * @param username    user performing the action (mandatory, must be a
   *                    {@code MAINTAINER})
   * @return updated network
   * @throws ElementNotFoundException  when network or gateway does not exist
   * @throws UnauthorizedException     when user is missing or not authorized
   * @throws InvalidInputDataException when mandatory data are invalid
   */
  public Network connectGateway(String networkCode, String gatewayCode, String username)
      throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException;

  /**
   * Removes the association between a gateway and a network.
   *
   * @param networkCode network code (mandatory)
   * @param gatewayCode gateway code (mandatory)
   * @param username    user performing the action (mandatory, must be a
   *                    {@code MAINTAINER})
   * @return updated network
   * @throws ElementNotFoundException  when network or gateway does not exist
   * @throws UnauthorizedException     when user is missing or not authorized
   * @throws InvalidInputDataException when mandatory data are invalid
   */
  public Network disconnectGateway(String networkCode, String gatewayCode, String username)
      throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException;

  /**
   * Returns sensors associated with a gateway.
   *
   * @param gatewayCode gateway code (mandatory)
   * @return collection of sensors linked to the gateway
   * @throws InvalidInputDataException when mandatory data are invalid
   * @throws ElementNotFoundException  when the gateway does not exist
   */
  public Collection<Sensor> getGatewaySensors(String gatewayCode)
      throws InvalidInputDataException, ElementNotFoundException;

  /**
   * Associates a sensor to a gateway.
   *
   * @param sensorCode  sensor code (mandatory)
   * @param gatewayCode gateway code (mandatory)
   * @param username    user performing the action (mandatory, must be a
   *                    {@code MAINTAINER})
   * @return updated gateway
   * @throws ElementNotFoundException  when sensor or gateway does not exist
   * @throws UnauthorizedException     when user is missing or not authorized
   * @throws InvalidInputDataException when mandatory data are invalid
   */
  public Gateway connectSensor(String sensorCode, String gatewayCode, String username)
      throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException;

  /**
   * Removes the association between a sensor and a gateway.
   *
   * @param sensorCode  sensor code (mandatory)
   * @param gatewayCode gateway code (mandatory)
   * @param username    user performing the action (mandatory, must be a
   *                    {@code MAINTAINER})
   * @return updated gateway
   * @throws ElementNotFoundException  when sensor or gateway does not exist
   * @throws UnauthorizedException     when user is missing or not authorized
   * @throws InvalidInputDataException when mandatory data are invalid
   */
  public Gateway disconnectSensor(String sensorCode, String gatewayCode, String username)
      throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException;

}
