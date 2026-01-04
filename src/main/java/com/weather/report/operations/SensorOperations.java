package com.weather.report.operations;

import java.util.Collection;

import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.IdAlreadyInUseException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.reports.SensorReport;
import com.weather.report.model.ThresholdType;
import com.weather.report.model.entities.Sensor;
import com.weather.report.model.entities.Threshold;

/**
 * Operations for managing sensors and their thresholds (R3) and producing the
 * sensor
 * report. Implementations must enforce validation rules, auditing, and deletion
 * notifications via {@code AlertingService}.
 */
public interface SensorOperations {
  /**
   * Creates a sensor with the provided attributes.
   *
   * @param code        sensor unique code (mandatory, must follow
   *                    {@code S_######})
   * @param name        sensor name (optional)
   * @param description sensor description (optional)
   * @param username    user performing the action (mandatory, must be a
   *                    {@code MAINTAINER})
   * @return created sensor
   * @throws IdAlreadyInUseException   when a sensor with the same code exists
   * @throws InvalidInputDataException when mandatory data are invalid
   * @throws UnauthorizedException     when user is missing or not authorized
   */
  public Sensor createSensor(String code, String name, String description, String username)
      throws IdAlreadyInUseException, InvalidInputDataException, UnauthorizedException;

  /**
   * Updates name/description of an existing sensor.
   *
   * @param code        sensor code (mandatory)
   * @param name        new name (optional)
   * @param description new description (optional)
   * @param username    user performing the action (mandatory, must be a
   *                    {@code MAINTAINER})
   * @return updated sensor
   * @throws InvalidInputDataException when mandatory data are invalid
   * @throws ElementNotFoundException  when the sensor does not exist
   * @throws UnauthorizedException     when user is missing or not authorized
   */
  public Sensor updateSensor(String code, String name, String description, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException;

  /**
   * Deletes a sensor and triggers deletion notification.
   *
   * @param code     sensor code (mandatory)
   * @param username user performing the action (mandatory, must be a
   *                 {@code MAINTAINER})
   * @return deleted sensor
   * @throws InvalidInputDataException when mandatory data are invalid
   * @throws ElementNotFoundException  when the sensor does not exist
   * @throws UnauthorizedException     when user is missing or not authorized
   */
  public Sensor deleteSensor(String code, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException;

  /**
   * Retrieves sensors by code. When invoked with no arguments, returns all
   * sensors.
   * Unknown codes are ignored.
   *
   * @param sensorCodes list of codes to fetch (optional)
   * @return collection of sensors found
   */
  public Collection<Sensor> getSensors(String... sensorCodes);

  /**
   * Creates a threshold for a sensor.
   *
   * @param sensorCode target sensor code (mandatory)
   * @param type       comparison type (mandatory)
   * @param value      threshold numeric value
   * @param username   user performing the action (mandatory, must be a
   *                   {@code MAINTAINER})
   * @return created threshold
   * @throws InvalidInputDataException when mandatory data are invalid
   * @throws ElementNotFoundException  when the sensor does not exist
   * @throws IdAlreadyInUseException   when a threshold already exists for the
   *                                   sensor
   * @throws UnauthorizedException     when user is missing or not authorized
   */
  public Threshold createThreshold(String sensorCode, ThresholdType type, double value, String username)
      throws InvalidInputDataException, ElementNotFoundException, IdAlreadyInUseException,
      UnauthorizedException;

  /**
   * Updates an existing threshold for a sensor.
   *
   * @param sensorCode target sensor code (mandatory)
   * @param type       comparison type (mandatory)
   * @param value      new threshold numeric value
   * @param username   user performing the action (mandatoy, must be a
   *                   {@code MAINTAINER})
   * @return updated threshold
   * @throws InvalidInputDataException when mandatory data are invalid
   * @throws ElementNotFoundException  when sensor or threshold does not exist
   * @throws UnauthorizedException     when user is missing or not authorized
   */
  public Threshold updateThreshold(String sensorCode, ThresholdType type, double value, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException;

  /**
   * Builds the report for a sensor in the given interval.
   *
   * @param code      sensor code (mandatory)
   * @param startDate inclusive lower bound in {@code WeatherReport.DATE_FORMAT}
   *                  (null for no bound)
   * @param endDate   inclusive upper bound in {@code WeatherReport.DATE_FORMAT}
   *                  (null for no bound)
   * @return computed sensor report
   * @throws InvalidInputDataException when manadtory data are invalid
   * @throws ElementNotFoundException  when the sensor does not exist
   */
  public SensorReport getSensorReport(String code, String startDate, String endDate)
      throws InvalidInputDataException, ElementNotFoundException;
}
