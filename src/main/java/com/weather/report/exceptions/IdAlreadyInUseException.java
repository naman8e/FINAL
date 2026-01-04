package com.weather.report.exceptions;

/// Exception thrown when an attempt is made to create a new element using a unique code that is already present in the system.
public class IdAlreadyInUseException extends WeatherReportException {

  public IdAlreadyInUseException(String message) {
    super(message, WeatherReportException.ID_ALREADY_IN_USE);
  }

}
