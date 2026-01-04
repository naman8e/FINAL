package com.weather.report.services;

import com.weather.report.model.ThresholdType;
import com.weather.report.model.entities.*;
import com.weather.report.repositories.CRUDRepository;
import com.weather.report.repositories.MeasurementRepository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DataImportingService {

  private static final Logger logger = LogManager.getLogger(DataImportingService.class);

  private static final int EXPECTED_CSV_COLUMNS = 5;

  private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private DataImportingService() {
  }

  public static void storeMeasurements(String filePath) {
    if (filePath == null || filePath.isBlank()) {
      throw new IllegalArgumentException("File path cannot be null or empty");
    }

    logger.info("Starting measurement import from file: {}", filePath);

    File file = resolveFilePath(filePath);

    if (!file.exists()) {
      throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
    }

    if (!file.canRead()) {
      throw new IllegalArgumentException("File is not readable: " + file.getAbsolutePath());
    }

    MeasurementRepository repository = new MeasurementRepository();

    int totalRows = 0;
    int successfulRows = 0;
    int skippedRows = 0;

    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

      String headerLine = reader.readLine();
      if (headerLine == null) {
        logger.warn("File is empty: {}", filePath);
        return;
      }

      String line;
      int lineNumber = 1;

      while ((line = reader.readLine()) != null) {
        lineNumber++;
        totalRows++;

        if (line.isBlank()) {
          skippedRows++;
          continue;
        }

        try {
          Measurement measurement = parseCSVLine(line, lineNumber);
          Measurement savedMeasurement = repository.create(measurement);
          successfulRows++;
          checkMeasurement(savedMeasurement);

        } catch (InvalidCSVLineException e) {
          logger.warn("Skipping invalid CSV line {}: {}", lineNumber, e.getMessage());
          skippedRows++;
        } catch (Exception e) {
          logger.error("Error saving measurement from line {}: {}", lineNumber, e.getMessage(), e);
          skippedRows++;
        }
      }

      logger.info(
          "Import complete. Total rows: {}, Successful: {}, Skipped: {}",
          totalRows, successfulRows, skippedRows);

    } catch (IOException e) {
      String errorMessage = "Failed to read CSV file: " + filePath;
      logger.error(errorMessage, e);
      throw new RuntimeException(errorMessage, e);
    }
  }

  private static File resolveFilePath(String filePath) {
    try {
      String decodedPath = URLDecoder.decode(filePath, StandardCharsets.UTF_8.name());
      boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

      if (decodedPath.startsWith("file:/")) {
        decodedPath = decodedPath.substring(6);
      }

      if (isWindows && decodedPath.startsWith("/") && decodedPath.length() > 2) {
        if (Character.isLetter(decodedPath.charAt(1)) && decodedPath.charAt(2) == ':') {
          decodedPath = decodedPath.substring(1);
        }
      }

      return new File(decodedPath);

    } catch (Exception e) {
      logger.warn("Failed to decode path '{}', using as-is: {}", filePath, e.getMessage());
      return new File(filePath);
    }
  }

  private static Measurement parseCSVLine(String line, int lineNumber)
      throws InvalidCSVLineException {

    String[] parts = line.split(",");

    if (parts.length != EXPECTED_CSV_COLUMNS) {
      throw new InvalidCSVLineException(
          String.format("Expected %d columns, found %d",
              EXPECTED_CSV_COLUMNS, parts.length));
    }

    try {
      String dateString = parts[0].trim();
      String networkCode = parts[1].trim();
      String gatewayCode = parts[2].trim();
      String sensorCode = parts[3].trim();
      String valueString = parts[4].trim();

      if (networkCode.isEmpty() || gatewayCode.isEmpty() || sensorCode.isEmpty()) {
        throw new InvalidCSVLineException("Network, gateway, or sensor code is empty");
      }

      LocalDateTime timestamp;
      try {
        timestamp = LocalDateTime.parse(dateString, CSV_DATE_FORMATTER);
      } catch (DateTimeParseException e) {
        throw new InvalidCSVLineException("Invalid date format: " + dateString);
      }

      double value;
      try {
        value = Double.parseDouble(valueString);
      } catch (NumberFormatException e) {
        throw new InvalidCSVLineException("Invalid numeric value: " + valueString);
      }

      return new Measurement(networkCode, gatewayCode, sensorCode, value, timestamp);

    } catch (InvalidCSVLineException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidCSVLineException("Unexpected error parsing line: " + e.getMessage());
    }
  }

  private static void checkMeasurement(Measurement measurement) {
    CRUDRepository<Sensor, String> sensorRepository = new CRUDRepository<>(Sensor.class);

    try {
      Sensor currentSensor = sensorRepository.read().stream()
          .filter(sensor -> measurement.getSensorCode().equals(sensor.getCode()))
          .findFirst()
          .orElse(null);

      if (currentSensor == null) {
        return;
      }

      Threshold threshold = currentSensor.getThreshold();
      if (threshold == null) {
        return;
      }

      boolean isViolation = checkThresholdViolation(
          measurement.getValue(),
          threshold.getValue(),
          threshold.getType());

      if (isViolation) {
        CRUDRepository<Network, String> networkRepository = new CRUDRepository<>(Network.class);

        Network network = networkRepository.read(measurement.getNetworkCode());

        if (network != null && !network.getOperators().isEmpty()) {
          AlertingService.notifyThresholdViolation(
              network.getOperators(),
              currentSensor.getCode());
        }
      }

    } catch (Exception e) {
      logger.error(
          "Error checking threshold for measurement {}: {}",
          measurement.getSensorCode(), e.getMessage(), e);
    }
  }

  private static boolean checkThresholdViolation(
      double measuredValue,
      double thresholdValue,
      ThresholdType type) {

    final double EPSILON = 0.0001;

    return switch (type) {
      case GREATER_THAN -> measuredValue > thresholdValue;
      case LESS_THAN -> measuredValue < thresholdValue;
      case GREATER_OR_EQUAL -> measuredValue >= thresholdValue;
      case LESS_OR_EQUAL -> measuredValue <= thresholdValue;
      case EQUAL -> Math.abs(measuredValue - thresholdValue) < EPSILON;
      case NOT_EQUAL -> Math.abs(measuredValue - thresholdValue) >= EPSILON;
    };
  }

  private static class InvalidCSVLineException extends Exception {
    public InvalidCSVLineException(String message) {
      super(message);
    }
  }
}
