package com.weather.report.test.base;

import java.time.LocalDateTime;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;
import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.IdAlreadyInUseException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.exceptions.WeatherReportException;
import com.weather.report.reports.SensorReport;
import com.weather.report.model.ThresholdType;
import com.weather.report.model.entities.Sensor;
import com.weather.report.model.entities.Threshold;
import com.weather.report.services.AlertingService;
import com.weather.report.test.BasePersistenceTest;

public class Test_R3 extends BasePersistenceTest {

  @BeforeAll
  static void checkBranchForR3() {
    assumeRequirement(3);
  }

  @Test
  void createSensorShouldCreateSensor() throws Throwable {
    try (MockedStatic<AlertingService> alerting = mockStatic(AlertingService.class)) {
      LocalDateTime beforeCreate = LocalDateTime.now();
      Sensor sensor = facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);
      LocalDateTime afterCreate = LocalDateTime.now();

      Assertions.assertNotNull(sensor);
      Assertions.assertEquals(SENSOR_010101, sensor.getCode());
      Assertions.assertEquals(sensorName("1"), sensor.getName());
      Assertions.assertEquals(desc("1"), sensor.getDescription());
      Assertions.assertEquals(MAINTAINER_USERNAME, sensor.getCreatedBy());
      assertTimestampWithinTolerance(sensor.getCreatedAt(), beforeCreate, afterCreate);
      Assertions.assertNull(sensor.getModifiedBy());
      Assertions.assertNull(sensor.getModifiedAt());

      Collection<Sensor> sensors = facade.sensors().getSensors(SENSOR_010101);
      Assertions.assertEquals(1, sensors.size());
      Sensor loaded = sensors.iterator().next();
      Assertions.assertEquals(SENSOR_010101, loaded.getCode());
      Assertions.assertEquals(sensorName("1"), loaded.getName());
      Assertions.assertEquals(desc("1"), loaded.getDescription());
      Assertions.assertEquals(MAINTAINER_USERNAME, loaded.getCreatedBy());
      assertTimestampWithinTolerance(loaded.getCreatedAt(), beforeCreate, afterCreate);
      Assertions.assertNull(loaded.getModifiedBy());
      Assertions.assertNull(loaded.getModifiedAt());

      alerting.verifyNoInteractions();
    }
  }

  @Test
  void createSensorShouldFailWhenCodeIsNull() {
    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.sensors().createSensor(null, sensorName("1"), desc("1"), MAINTAINER_USERNAME));
  }

  @Test
  void createSensorShouldFailWhenCodeAlreadyExists() throws WeatherReportException {
    facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);

    Assertions.assertThrows(
        IdAlreadyInUseException.class,
        () -> facade.sensors().createSensor(SENSOR_010101, "Another sensor", "Another desc", MAINTAINER_USERNAME));
  }

  @Test
  void createSensorShouldFailWhenUserIsNotMaintainer() throws WeatherReportException {
    Assertions.assertThrows(
        UnauthorizedException.class,
        () -> facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), VIEWER_USERNAME));
  }

  @Test
  void updateSensorShouldModifyExistingSensor() throws WeatherReportException {
    try (MockedStatic<AlertingService> alerting = mockStatic(AlertingService.class)) {
      facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);

      LocalDateTime beforeUpdate = LocalDateTime.now();
      Sensor updated = facade.sensors().updateSensor(SENSOR_010101, UPDATED_NAME, UPDATED_DESC, UPDATER_USERNAME);
      LocalDateTime afterUpdate = LocalDateTime.now();

      Assertions.assertEquals(SENSOR_010101, updated.getCode());
      Assertions.assertEquals(UPDATED_NAME, updated.getName());
      Assertions.assertEquals(UPDATED_DESC, updated.getDescription());
      Assertions.assertEquals(MAINTAINER_USERNAME, updated.getCreatedBy());
      Assertions.assertEquals(UPDATER_USERNAME, updated.getModifiedBy());
      assertTimestampWithinTolerance(updated.getModifiedAt(), beforeUpdate, afterUpdate);

      Collection<Sensor> sensors = facade.sensors().getSensors(SENSOR_010101);
      Assertions.assertEquals(1, sensors.size());
      Sensor reloaded = sensors.iterator().next();
      Assertions.assertEquals(UPDATED_NAME, reloaded.getName());
      Assertions.assertEquals(UPDATED_DESC, reloaded.getDescription());
      Assertions.assertEquals(MAINTAINER_USERNAME, reloaded.getCreatedBy());
      Assertions.assertEquals(UPDATER_USERNAME, reloaded.getModifiedBy());
      assertTimestampWithinTolerance(reloaded.getModifiedAt(), beforeUpdate, afterUpdate);

      alerting.verifyNoInteractions();
    }
  }

  @Test
  void updateSensorShouldFailWhenCodeIsNull() {
    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.sensors().updateSensor(null, "Name", "Desc", MAINTAINER_USERNAME));
  }

  @Test
  void updateSensorShouldFailWhenSensorDoesNotExist() {
    Assertions.assertThrows(
        ElementNotFoundException.class,
        () -> facade.sensors().updateSensor(SENSOR_UNKNOWN, "Name", "Desc", MAINTAINER_USERNAME));
  }

  @Test
  void updateSensorShouldFailWhenUserIsNotMaintainer() throws WeatherReportException {
    facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);

    Assertions.assertThrows(
        UnauthorizedException.class,
        () -> facade.sensors().updateSensor(SENSOR_010101, UPDATED_NAME, UPDATED_DESC, VIEWER_USERNAME));
  }

  @Test
  void deleteSensorShouldRemoveExistingSensor() throws WeatherReportException {
    try (MockedStatic<AlertingService> alerting = mockStatic(AlertingService.class)) {
      facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);

      Sensor deleted = facade.sensors().deleteSensor(SENSOR_010101, MAINTAINER_USERNAME);
      Assertions.assertEquals(SENSOR_010101, deleted.getCode());

      Collection<Sensor> sensors = facade.sensors().getSensors(SENSOR_010101);
      Assertions.assertTrue(sensors.isEmpty());

      alerting.verify(() -> AlertingService.notifyDeletion(MAINTAINER_USERNAME, SENSOR_010101, Sensor.class));
    }
  }

  @Test
  void deleteSensorShouldFailWhenSensorDoesNotExist() {
    Assertions.assertThrows(
        ElementNotFoundException.class,
        () -> facade.sensors().deleteSensor(SENSOR_UNKNOWN, MAINTAINER_USERNAME));
  }

  @Test
  void deleteSensorShouldFailWhenUserIsNotMaintainer() throws WeatherReportException {
    facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);

    Assertions.assertThrows(
        UnauthorizedException.class,
        () -> facade.sensors().deleteSensor(SENSOR_010101, VIEWER_USERNAME));
  }

  @Test
  void getSensorsWithoutArgumentsShouldReturnAllSensors() throws WeatherReportException {
    facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);
    facade.sensors().createSensor(SENSOR_010102, sensorName("2"), desc("2"), MAINTAINER_USERNAME);

    Collection<Sensor> sensors = facade.sensors().getSensors();

    Assertions.assertEquals(2, sensors.size());
  }

  @Test
  void getSensorsWithCodesShouldReturnOnlyMatchingSensors() throws WeatherReportException {
    facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);
    facade.sensors().createSensor(SENSOR_010102, sensorName("2"), desc("2"), MAINTAINER_USERNAME);

    Collection<Sensor> sensors = facade.sensors().getSensors(SENSOR_010102);

    Assertions.assertEquals(1, sensors.size());
    Sensor sensor = sensors.iterator().next();
    Assertions.assertEquals(SENSOR_010102, sensor.getCode());
  }

  @Test
  void getSensorsWithNonExistingCodesShouldReturnEmptyCollection() throws WeatherReportException {
    facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);

    Collection<Sensor> sensors = facade.sensors().getSensors(SENSOR_UNKNOWN);

    Assertions.assertTrue(sensors.isEmpty());
  }

  @Test
  void createThresholdShouldCreateThreshold() throws WeatherReportException {
    facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);
    ThresholdType type = ThresholdType.values()[0];

    Threshold threshold = facade.sensors().createThreshold(SENSOR_010101, type, 10.0, MAINTAINER_USERNAME);

    Assertions.assertNotNull(threshold);
    Assertions.assertEquals(type, threshold.getType());
    Assertions.assertEquals(10.0, threshold.getValue());
  }

  @Test
  void createThresholdShouldFailWhenSensorCodeIsNull() {
    ThresholdType type = ThresholdType.values()[0];

    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.sensors().createThreshold(null, type, 10.0, MAINTAINER_USERNAME));
  }

  @Test
  void createThresholdShouldFailWhenSensorDoesNotExist() {
    ThresholdType type = ThresholdType.values()[0];

    Assertions.assertThrows(
        ElementNotFoundException.class,
        () -> facade.sensors().createThreshold(SENSOR_UNKNOWN, type, 10.0, MAINTAINER_USERNAME));
  }

  @Test
  void createThresholdShouldFailWhenSensorAlreadyHasThreshold() throws WeatherReportException {
    facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);
    ThresholdType type = ThresholdType.values()[0];

    facade.sensors().createThreshold(SENSOR_010101, type, 10.0, MAINTAINER_USERNAME);

    Assertions.assertThrows(
        IdAlreadyInUseException.class,
        () -> facade.sensors().createThreshold(SENSOR_010101, type, 20.0, MAINTAINER_USERNAME));
  }

  @Test
  void createThresholdShouldFailWhenUserIsNotMaintainer() throws WeatherReportException {
    facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);
    ThresholdType type = ThresholdType.values()[0];

    Assertions.assertThrows(
        UnauthorizedException.class,
        () -> facade.sensors().createThreshold(SENSOR_010101, type, 10.0, VIEWER_USERNAME));
  }

  @Test
  void updateThresholdShouldModifyExistingThreshold() throws WeatherReportException {
    facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);
    ThresholdType type = ThresholdType.values()[0];

    facade.sensors().createThreshold(SENSOR_010101, type, 10.0, MAINTAINER_USERNAME);

    Threshold updated = facade.sensors().updateThreshold(SENSOR_010101, type, 25.0, MAINTAINER_USERNAME);

    Assertions.assertEquals(type, updated.getType());
    Assertions.assertEquals(25.0, updated.getValue());
  }

  @Test
  void updateThresholdShouldFailWhenSensorCodeIsNull() {
    ThresholdType type = ThresholdType.values()[0];

    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.sensors().updateThreshold(null, type, 10.0, MAINTAINER_USERNAME));
  }

  @Test
  void updateThresholdShouldFailWhenSensorDoesNotExist() {
    ThresholdType type = ThresholdType.values()[0];

    Assertions.assertThrows(
        ElementNotFoundException.class,
        () -> facade.sensors().updateThreshold(SENSOR_UNKNOWN, type, 10.0, MAINTAINER_USERNAME));
  }

  @Test
  void updateThresholdShouldFailWhenSensorHasNoThreshold() throws WeatherReportException {
    facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);
    ThresholdType type = ThresholdType.values()[0];

    Assertions.assertThrows(
        ElementNotFoundException.class,
        () -> facade.sensors().updateThreshold(SENSOR_010101, type, 10.0, MAINTAINER_USERNAME));
  }

  @Test
  void updateThresholdShouldFailWhenUserIsNotMaintainer() throws WeatherReportException {
    facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);
    ThresholdType type = ThresholdType.values()[0];
    facade.sensors().createThreshold(SENSOR_010101, type, 10.0, MAINTAINER_USERNAME);

    Assertions.assertThrows(
        UnauthorizedException.class,
        () -> facade.sensors().updateThreshold(SENSOR_010101, type, 25.0, VIEWER_USERNAME));
  }

  @Test
  void sensorShouldTrackCreatorAndLastModifier() throws WeatherReportException {
    LocalDateTime beforeCreate = LocalDateTime.now();
    Sensor sensor = facade.sensors().createSensor(SENSOR_424242, sensorName("meta"), "Desc meta", MAINTAINER_USERNAME);
    LocalDateTime afterCreate = LocalDateTime.now();
    Assertions.assertEquals(MAINTAINER_USERNAME, sensor.getCreatedBy());
    assertTimestampWithinTolerance(sensor.getCreatedAt(), beforeCreate, afterCreate);
    Assertions.assertNull(sensor.getModifiedBy());
    Assertions.assertNull(sensor.getModifiedAt());

    LocalDateTime beforeFirstUpdate = LocalDateTime.now();
    Sensor firstUpdate = facade.sensors().updateSensor(SENSOR_424242, UPDATED_ONCE, DESC_UPDATED_ONCE,
        UPDATER_USERNAME);
    LocalDateTime afterFirstUpdate = LocalDateTime.now();
    Assertions.assertEquals(MAINTAINER_USERNAME, firstUpdate.getCreatedBy());
    Assertions.assertEquals(UPDATER_USERNAME, firstUpdate.getModifiedBy());
    assertTimestampWithinTolerance(firstUpdate.getModifiedAt(), beforeFirstUpdate, afterFirstUpdate);

    LocalDateTime beforeSecondUpdate = LocalDateTime.now();
    Sensor secondUpdate = facade.sensors().updateSensor(SENSOR_424242, UPDATED_TWICE, DESC_UPDATED_TWICE,
        MAINTAINER_USERNAME);
    LocalDateTime afterSecondUpdate = LocalDateTime.now();
    Assertions.assertEquals(MAINTAINER_USERNAME, secondUpdate.getCreatedBy());
    Assertions.assertEquals(MAINTAINER_USERNAME, secondUpdate.getModifiedBy());
    assertTimestampWithinTolerance(secondUpdate.getModifiedAt(), beforeSecondUpdate, afterSecondUpdate);
    Assertions.assertTrue(
        !secondUpdate.getModifiedAt().isBefore(firstUpdate.getModifiedAt().minusSeconds(TIME_TOLERANCE_SECONDS)));
  }

  @Test
  void getSensorReportShouldFailWhenCodeIsNull() {
    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.sensors().getSensorReport(null, null, null));
  }

  @Test
  void getSensorReportShouldFailWhenSensorDoesNotExist() {
    Assertions.assertThrows(
        ElementNotFoundException.class,
        () -> facade.sensors().getSensorReport(SENSOR_UNKNOWN, null, null));
  }

  @Test
  void getSensorReportWithoutMeasurementsShouldReturnEmptyStats() throws WeatherReportException {
    facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);

    SensorReport report = facade.sensors().getSensorReport(SENSOR_010101, null, null);

    Assertions.assertNotNull(report);
    Assertions.assertEquals(SENSOR_010101, report.getCode());
    Assertions.assertNull(report.getStartDate());
    Assertions.assertNull(report.getEndDate());
    Assertions.assertEquals(0, report.getNumberOfMeasurements());
    Assertions.assertTrue(report.getOutliers().isEmpty());
    Assertions.assertTrue(report.getHistogram().isEmpty());
  }

  @Test
  void getSensorReportWithoutDatesShouldReturnStatsAndNoDates() throws WeatherReportException {
    facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);
    facade.importDataFromFile(getClass().getClassLoader().getResource("csv/S_111.csv").getPath());

    SensorReport report = facade.sensors().getSensorReport(SENSOR_010101, null, null);

    Assertions.assertNotNull(report);
    Assertions.assertEquals(SENSOR_010101, report.getCode());
    Assertions.assertNull(report.getStartDate());
    Assertions.assertNull(report.getEndDate());
    Assertions.assertEquals(166, report.getNumberOfMeasurements());
    Assertions.assertNotEquals(0.0, report.getMean());
    Assertions.assertNotEquals(0.0, report.getVariance());
    Assertions.assertNotEquals(0.0, report.getStdDev());
    Assertions.assertFalse(report.getHistogram().isEmpty());
  }

  @Test
  void getSensorReportWithDatesShouldReturnStatsAndDates() throws WeatherReportException {
    facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);
    facade.importDataFromFile(getClass().getClassLoader().getResource("csv/S_111.csv").getPath());

    SensorReport report = facade.sensors().getSensorReport(SENSOR_010101, "2025-11-16 10:00:00", "2025-11-16 20:00:00");

    Assertions.assertNotNull(report);
    Assertions.assertEquals(SENSOR_010101, report.getCode());
    Assertions.assertEquals("2025-11-16 10:00:00", report.getStartDate());
    Assertions.assertEquals("2025-11-16 20:00:00", report.getEndDate());
    Assertions.assertEquals(11, report.getNumberOfMeasurements());
    Assertions.assertNotEquals(0.0, report.getMean());
    Assertions.assertNotEquals(0.0, report.getVariance());
    Assertions.assertNotEquals(0.0, report.getStdDev());
    Assertions.assertFalse(report.getHistogram().isEmpty());
  }

  @Test
  void getSensorReportWithOneDateShouldReturnStatsAndOneDate() throws WeatherReportException {
    facade.sensors().createSensor(SENSOR_010101, sensorName("1"), desc("1"), MAINTAINER_USERNAME);
    facade.importDataFromFile(getClass().getClassLoader().getResource("csv/S_111.csv").getPath());

    SensorReport report = facade.sensors().getSensorReport(SENSOR_010101, null, "2025-11-16 20:00:00");

    Assertions.assertNotNull(report);
    Assertions.assertEquals(SENSOR_010101, report.getCode());
    Assertions.assertNull(report.getStartDate());
    Assertions.assertEquals("2025-11-16 20:00:00", report.getEndDate());
    Assertions.assertEquals(13, report.getNumberOfMeasurements());
    Assertions.assertTrue(report.getOutliers().isEmpty());
    Assertions.assertNotEquals(0.0, report.getMean());
    Assertions.assertNotEquals(0.0, report.getVariance());
    Assertions.assertNotEquals(0.0, report.getStdDev());
    Assertions.assertFalse(report.getHistogram().isEmpty());

    report = facade.sensors().getSensorReport(SENSOR_010101, "2025-11-22 20:00:00", null);

    Assertions.assertNotNull(report);
    Assertions.assertEquals(SENSOR_010101, report.getCode());
    Assertions.assertEquals("2025-11-22 20:00:00", report.getStartDate());
    Assertions.assertNull(report.getEndDate());
    Assertions.assertEquals(10, report.getNumberOfMeasurements());
    Assertions.assertTrue(report.getOutliers().isEmpty());
    Assertions.assertNotEquals(0.0, report.getMean());
    Assertions.assertNotEquals(0.0, report.getVariance());
    Assertions.assertNotEquals(0.0, report.getStdDev());
    Assertions.assertFalse(report.getHistogram().isEmpty());
  }

}
