package com.weather.report.operations;

import java.util.Collection;

import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.IdAlreadyInUseException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.entities.Network;
import com.weather.report.model.entities.Operator;
import com.weather.report.reports.NetworkReport;

/**
 * Operations for managing networks and operators (R1) and for producing the
 * network
 * report. Implementations must enforce validation rules, auditing on
 * timestamped
 * fields, and deletion notifications via {@code AlertingService}.
 */
public interface NetworkOperations {

  /**
   * Creates a network with the provided attributes.
   *
   * @param code        network unique code (mandatory, must follow
   *                    {@code NET_##})
   * @param name        network name (optional)
   * @param description network description (optional)
   * @param username    user performing the action (mandatory, must be a
   *                    {@code MAINTAINER})
   * @return created network
   * @throws IdAlreadyInUseException   when a network with the same code exists
   * @throws InvalidInputDataException when mandatory data are invalid
   * @throws UnauthorizedException     when user is missing or not authorized
   */
  public Network createNetwork(String code, String name, String description, String username)
      throws IdAlreadyInUseException, InvalidInputDataException, UnauthorizedException;

  /**
   * Updates name/description of an existing network.
   *
   * @param code        network code (mandatory)
   * @param name        new name (optional)
   * @param description new description (optional)
   * @param username    user performing the action (mandatory, must be a
   *                    {@code MAINTAINER})
   * @return updated network
   * @throws InvalidInputDataException when mandatory data are invalid
   * @throws ElementNotFoundException  when the network does not exist
   * @throws UnauthorizedException     when user is missing or not authorized
   */
  public Network updateNetwork(String code, String name, String description, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException;

  /**
   * Deletes a network and triggers deletion notification.
   *
   * @param code     network code (mandatory)
   * @param username user performing the action (mandatory, must be a
   *                 {@code MAINTAINER})
   * @return deleted network
   * @throws InvalidInputDataException when mandatory data are invalid
   * @throws ElementNotFoundException  when the network does not exist
   * @throws UnauthorizedException     when user is missing or not authorized
   */
  public Network deleteNetwork(String code, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException;

  /**
   * Retrieves networks by code. When invoked with no arguments, returns all
   * networks.
   * Unknown codes are ignored.
   *
   * @param codes list of codes to fetch (optional)
   * @return collection of networks found
   */
  public Collection<Network> getNetworks(String... codes);

  /**
   * Creates an operator identified by email.
   *
   * @param firstName   operator first name (mandatory)
   * @param lastName    operator last name (mandatory)
   * @param email       operator unique email (mandatory)
   * @param phoneNumber operator phone (optional)
   * @param username    user performing the action (mandatory, must be a
   *                    {@code MAINTAINER})
   * @return created operator
   * @throws InvalidInputDataException when mandatory data are invalid
   * @throws IdAlreadyInUseException   when an operator with the same email exists
   * @throws UnauthorizedException     when user is missing or not authorized
   */
  public Operator createOperator(String firstName, String lastName, String email, String phoneNumber,
      String username)
      throws InvalidInputDataException, IdAlreadyInUseException, UnauthorizedException;

  /**
   * Associates an existing operator to a network.
   *
   * @param networkCode   target network code (mandatory)
   * @param operatorEmail operator email to link (mandatory)
   * @param username      user performing the action (mandatory, must be a
   *                      {@code MAINTAINER})
   * @return updated network
   * @throws ElementNotFoundException  when network or operator does not exist
   * @throws InvalidInputDataException when mandatory data are invalid
   * @throws UnauthorizedException     when user is missing or not authorized
   */
  public Network addOperatorToNetwork(String networkCode, String operatorEmail, String username)
      throws ElementNotFoundException, InvalidInputDataException, UnauthorizedException;

  /**
   * Builds the report for a network in the given interval.
   *
   * @param code      network code (mandatory)
   * @param startDate inclusive lower bound in {@code WeatherReport.DATE_FORMAT}
   *                  (null for no bound)
   * @param endDate   inclusive upper bound in {@code WeatherReport.DATE_FORMAT}
   *                  (null for no bound)
   * @return computed network report
   * @throws InvalidInputDataException when mandatory data are invalid
   * @throws ElementNotFoundException  when the network does not exist
   */
  public NetworkReport getNetworkReport(String code, String startDate, String endDate)
      throws InvalidInputDataException, ElementNotFoundException;
}
