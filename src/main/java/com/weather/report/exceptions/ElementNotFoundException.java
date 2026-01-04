package com.weather.report.exceptions;

/// Exception thrown when the code of an element that is not contained in the system is provided.
public class ElementNotFoundException extends WeatherReportException {

  public ElementNotFoundException(String message) {
    super(message, WeatherReportException.NOT_FOUND);
  }

}
