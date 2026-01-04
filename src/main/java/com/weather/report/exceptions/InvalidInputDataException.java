package com.weather.report.exceptions;

public class InvalidInputDataException extends WeatherReportException {

  /// Exception thrown when invalid, missing or non-conforming data are provided for mandatory attributes.  
  public InvalidInputDataException(String message) {
    super(message, WeatherReportException.INVALID_INPUT_DATA);
  }

}
