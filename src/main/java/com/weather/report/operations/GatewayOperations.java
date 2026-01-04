package com.weather.report.operations;

import java.util.Collection;

import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.IdAlreadyInUseException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.entities.Gateway;
import com.weather.report.model.entities.Parameter;
import com.weather.report.reports.GatewayReport;

/**
 * Operations for managing gateways and their parameters (R2), plus generation
 * of the
 * gateway report. Implementations must enforce validation rules and auditing
 * described
 * in the README and notify deletions through {@code AlertingService}.
 */
public interface GatewayOperations {
  /**
   * Creates a gateway with the provided attributes.
   *
   * @param code        gateway unique code (mandatory, must follow
   *                    {@code GW_####})
   * @param name        gateway name (optional)
   * @param description gateway description (optional)
   * @param username    user performing the action (mandatory, must be a
   *                    {@code MAINTAINER})
   * @return created gateway
   * @throws IdAlreadyInUseException   when a gateway with the same code exists
   * @throws InvalidInputDataException when mandatory data are invalid
   * @throws UnauthorizedException     when user is missing or has insufficient
   *                                   rights
   */
  public Gateway createGateway(String code, String name, String description, String username)
      throws IdAlreadyInUseException, InvalidInputDataException, UnauthorizedException;

  /**
   * Updates name/description of an existing gateway.
   *
   * @param code        gateway code (mandatory)
   * @param name        new name (optional)
   * @param description new description (optional)
   * @param username    user performing the action (mandatory, must be a
   *                    {@code MAINTAINER})
   * @return updated gateway
   * @throws InvalidInputDataException when mandatory data are invalid
   * @throws ElementNotFoundException  when the gateway does not exist
   * @throws UnauthorizedException     when user is missing or not authorized
   */
  public Gateway updateGateway(String code, String name, String description, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException;

  /**
   * Deletes a gateway and triggers deletion notification.
   *
   * @param code     gateway code (mandatory)
   * @param username user performing the action (mandatory, must be a
   *                 {@code MAINTAINER})
   * @return deleted gateway
   * @throws InvalidInputDataException when mandatory data are invalid
   * @throws ElementNotFoundException  when the gateway does not exist
   * @throws UnauthorizedException     when user is missing or not authorized
   */
  public Gateway deleteGateway(String code, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException;

  /**
   * Retrieves gateways by code. When invoked with no arguments, returns all
   * gateways.
   * Unknown codes are ignored.
   *
   * @param gatewayCodes list of codes to fetch (optional)
   * @return collection of gateways found
   */
  public Collection<Gateway> getGateways(String... gatewayCodes);

  /**
   * Creates a parameter within a gateway.
   *
   * @param gatewayCode target gateway code (mandatory)
   * @param code        parameter code (mandatory, unique inside the gateway)
   * @param name        parameter name (optional)
   * @param description parameter description (optional)
   * @param value       numeric value
   * @param username    user performing the action (mandatory, must be a
   *                    {@code MAINTAINER})
   * @return created parameter
   * @throws IdAlreadyInUseException   when the parameter code already exists in
   *                                   gateway
   * @throws InvalidInputDataException when mandatory data are invalid
   * @throws ElementNotFoundException  when gateway is missing
   * @throws UnauthorizedException     when user is missing or not authorized
   */
  public Parameter createParameter(String gatewayCode, String code, String name, String description, double value,
      String username)
      throws IdAlreadyInUseException, InvalidInputDataException, ElementNotFoundException,
      UnauthorizedException;

  /**
   * Updates the value of an existing parameter.
   *
   * @param gatewayCode target gateway code (mandatory)
   * @param code        parameter code (optional)
   * @param value       new numeric value (optional)
   * @param username    user performing the action (mandatory, must be a
   *                    {@code MAINTAINER})
   * @return updated parameter
   * @throws InvalidInputDataException when a provided code is null
   * @throws ElementNotFoundException  when gateway or parameter does not exist
   * @throws UnauthorizedException     when user is missing or not authorized
   */
  public Parameter updateParameter(String gatewayCode, String code, double value, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException;

  /**
   * Builds the report for a gateway in the given interval.
   *
   * @param code      gateway code (mandatory)
   * @param startDate inclusive lower bound in {@code WeatherReport.DATE_FORMAT}
   *                  (null for no bound)
   * @param endDate   inclusive upper bound in {@code WeatherReport.DATE_FORMAT}
   *                  (null for no bound)
   * @return computed gateway report
   * @throws ElementNotFoundException  when the gateway does not exist
   * @throws InvalidInputDataException when mandatory data are invalid
   */
  public GatewayReport getGatewayReport(String code, String startDate, String endDate)
      throws ElementNotFoundException, InvalidInputDataException;
}
