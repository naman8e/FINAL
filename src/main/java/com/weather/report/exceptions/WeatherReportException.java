package com.weather.report.exceptions;

public class WeatherReportException extends Exception {
  public static final int NOT_FOUND = 100;
  public static final int INVALID_INPUT_DATA = 200;
  public static final int ID_ALREADY_IN_USE = 300;
  public static final int UNAUTHORIZED = 400;

  private final int errorCode;

  WeatherReportException(String message, int errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public int getErrorCode() {
    return this.errorCode;
  }

}
