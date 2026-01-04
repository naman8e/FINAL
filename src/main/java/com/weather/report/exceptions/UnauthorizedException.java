package com.weather.report.exceptions;

/// It is thrown when the username passed to the operation:
///
/// - does not correspond to any existing user, or
/// - corresponds to a user who does not possess the required permissions to execute the operation.
public class UnauthorizedException extends WeatherReportException {

  public UnauthorizedException(String message) {
    super(message, WeatherReportException.UNAUTHORIZED);
  }

}
