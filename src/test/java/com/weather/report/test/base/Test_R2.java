package com.weather.report.test.base;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.IdAlreadyInUseException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.exceptions.WeatherReportException;
import com.weather.report.reports.GatewayReport;
import com.weather.report.repositories.CRUDRepository;
import com.weather.report.model.entities.Gateway;
import com.weather.report.model.entities.Parameter;
import com.weather.report.model.entities.Sensor;
import com.weather.report.services.AlertingService;
import com.weather.report.test.BasePersistenceTest;

public class Test_R2 extends BasePersistenceTest {

  @BeforeAll
  static void checkBranchForR2() {
    assumeRequirement(2);
  }

  @Test
  void createGatewayShouldCreateGateway() throws WeatherReportException {
    try (MockedStatic<AlertingService> alerting = mockStatic(AlertingService.class)) {
      LocalDateTime beforeCreate = LocalDateTime.now();
      Gateway gateway = facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);
      LocalDateTime afterCreate = LocalDateTime.now();

      Assertions.assertNotNull(gateway);
      Assertions.assertEquals(GW_0101, gateway.getCode());
      Assertions.assertEquals(gatewayName("1"), gateway.getName());
      Assertions.assertEquals(desc("1"), gateway.getDescription());
      Assertions.assertEquals(MAINTAINER_USERNAME, gateway.getCreatedBy());
      assertTimestampWithinTolerance(gateway.getCreatedAt(), beforeCreate, afterCreate);
      Assertions.assertNull(gateway.getModifiedBy());
      Assertions.assertNull(gateway.getModifiedAt());

      Collection<Gateway> gateways = facade.gateways().getGateways(GW_0101);
      Assertions.assertEquals(1, gateways.size());
      Gateway loaded = gateways.iterator().next();
      Assertions.assertEquals(GW_0101, loaded.getCode());
      Assertions.assertEquals(gatewayName("1"), loaded.getName());
      Assertions.assertEquals(desc("1"), loaded.getDescription());
      Assertions.assertEquals(MAINTAINER_USERNAME, loaded.getCreatedBy());
      assertTimestampWithinTolerance(loaded.getCreatedAt(), beforeCreate, afterCreate);
      Assertions.assertNull(loaded.getModifiedBy());
      Assertions.assertNull(loaded.getModifiedAt());

      alerting.verifyNoInteractions();
    }
  }

  @Test
  void createGatewayShouldFailWhenCodeIsNull() {
    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.gateways().createGateway(null, gatewayName("1"), desc("1"), MAINTAINER_USERNAME));
  }

  @Test
  void createGatewayShouldFailWhenCodeAlreadyExists() throws WeatherReportException {
    facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);

    Assertions.assertThrows(
        IdAlreadyInUseException.class,
        () -> facade.gateways().createGateway(GW_0101, "Another gateway", "Another desc", MAINTAINER_USERNAME));
  }

  @Test
  void createGatewayShouldFailWhenUserIsNotMaintainer() {
    Assertions.assertThrows(
        UnauthorizedException.class,
        () -> facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), VIEWER_USERNAME));
  }

  @Test
  void updateGatewayShouldModifyExistingGateway() throws WeatherReportException {
    try (MockedStatic<AlertingService> alerting = mockStatic(AlertingService.class)) {
      facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);

      LocalDateTime beforeUpdate = LocalDateTime.now();
      Gateway updated = facade.gateways().updateGateway(GW_0101, UPDATED_NAME, UPDATED_DESC, UPDATER_USERNAME);
      LocalDateTime afterUpdate = LocalDateTime.now();

      Assertions.assertEquals(GW_0101, updated.getCode());
      Assertions.assertEquals(UPDATED_NAME, updated.getName());
      Assertions.assertEquals(UPDATED_DESC, updated.getDescription());
      Assertions.assertEquals(MAINTAINER_USERNAME, updated.getCreatedBy());
      Assertions.assertEquals(UPDATER_USERNAME, updated.getModifiedBy());
      assertTimestampWithinTolerance(updated.getModifiedAt(), beforeUpdate, afterUpdate);

      Collection<Gateway> gateways = facade.gateways().getGateways(GW_0101);
      Assertions.assertEquals(1, gateways.size());
      Gateway reloaded = gateways.iterator().next();
      Assertions.assertEquals(UPDATED_NAME, reloaded.getName());
      Assertions.assertEquals(UPDATED_DESC, reloaded.getDescription());
      Assertions.assertEquals(MAINTAINER_USERNAME, reloaded.getCreatedBy());
      Assertions.assertEquals(UPDATER_USERNAME, reloaded.getModifiedBy());
      assertTimestampWithinTolerance(reloaded.getModifiedAt(), beforeUpdate, afterUpdate);

      alerting.verifyNoInteractions();
    }
  }

  @Test
  void updateGatewayShouldFailWhenCodeIsNull() {
    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.gateways().updateGateway(null, "Name", "Desc", MAINTAINER_USERNAME));
  }

  @Test
  void updateGatewayShouldFailWhenGatewayDoesNotExist() {
    Assertions.assertThrows(
        ElementNotFoundException.class,
        () -> facade.gateways().updateGateway(GW_UNKNOWN, "Name", "Desc", MAINTAINER_USERNAME));
  }

  @Test
  void updateGatewayShouldFailWhenUserIsNotMaintainer() throws WeatherReportException {
    facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);

    Assertions.assertThrows(
        UnauthorizedException.class,
        () -> facade.gateways().updateGateway(GW_0101, UPDATED_NAME, UPDATED_DESC, VIEWER_USERNAME));
  }

  @Test
  void deleteGatewayShouldRemoveExistingGateway() throws WeatherReportException {
    try (MockedStatic<AlertingService> alerting = mockStatic(AlertingService.class)) {
      facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);

      Gateway deleted = facade.gateways().deleteGateway(GW_0101, MAINTAINER_USERNAME);
      Assertions.assertEquals(GW_0101, deleted.getCode());

      Collection<Gateway> gateways = facade.gateways().getGateways(GW_0101);
      Assertions.assertTrue(gateways.isEmpty());

      alerting.verify(() -> AlertingService.notifyDeletion(MAINTAINER_USERNAME, GW_0101, Gateway.class));
    }
  }

  @Test
  void deleteGatewayShouldFailWhenGatewayDoesNotExist() {
    Assertions.assertThrows(
        ElementNotFoundException.class,
        () -> facade.gateways().deleteGateway(GW_UNKNOWN, MAINTAINER_USERNAME));
  }

  @Test
  void deleteGatewayShouldFailWhenUserIsNotMaintainer() throws WeatherReportException {
    facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);

    Assertions.assertThrows(
        UnauthorizedException.class,
        () -> facade.gateways().deleteGateway(GW_0101, VIEWER_USERNAME));
  }

  @Test
  void getGatewaysWithoutArgumentsShouldReturnAllGateways() throws WeatherReportException {
    facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);
    facade.gateways().createGateway(GW_0102, gatewayName("2"), desc("2"), MAINTAINER_USERNAME);

    Collection<Gateway> gateways = facade.gateways().getGateways();

    Assertions.assertEquals(2, gateways.size());
  }

  @Test
  void getGatewaysWithCodesShouldReturnOnlyMatchingGateways() throws WeatherReportException {
    facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);
    facade.gateways().createGateway(GW_0102, gatewayName("2"), desc("2"), MAINTAINER_USERNAME);

    Collection<Gateway> gateways = facade.gateways().getGateways(GW_0102);

    Assertions.assertEquals(1, gateways.size());
    Gateway gateway = gateways.iterator().next();
    Assertions.assertEquals(GW_0102, gateway.getCode());
  }

  @Test
  void getGatewaysWithNonExistingCodesShouldReturnEmptyCollection() throws WeatherReportException {
    facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);

    Collection<Gateway> gateways = facade.gateways().getGateways(GW_UNKNOWN);

    Assertions.assertTrue(gateways.isEmpty());
  }

  @Test
  void createParameterShouldCreateParameter() throws WeatherReportException {
    facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);

    Parameter parameter = facade.gateways().createParameter(GW_0101, PARAMETER_P01, "Param 1", "Desc P1", 10.5,
        MAINTAINER_USERNAME);

    Assertions.assertNotNull(parameter);
    Assertions.assertEquals(PARAMETER_P01, parameter.getCode());
    Assertions.assertEquals("Param 1", parameter.getName());
    Assertions.assertEquals("Desc P1", parameter.getDescription());
    Assertions.assertEquals(10.5, parameter.getValue());
  }

  @Test
  void createParameterShouldFailWhenGatewayCodeIsNull() {
    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.gateways().createParameter(null, PARAMETER_P01, "Param 1", "Desc", 10.0, MAINTAINER_USERNAME));
  }

  @Test
  void createParameterShouldFailWhenParameterCodeIsNull() {
    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.gateways().createParameter(GW_0101, null, "Param 1", "Desc", 10.0, MAINTAINER_USERNAME));
  }

  @Test
  void createParameterShouldFailWhenCodeAlreadyExistsInSameGateway() throws WeatherReportException {
    facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);
    facade.gateways().createParameter(GW_0101, PARAMETER_P01, "Param 1", "Desc 1", 10.0, MAINTAINER_USERNAME);

    Assertions.assertThrows(
        IdAlreadyInUseException.class,
        () -> facade.gateways().createParameter(GW_0101, PARAMETER_P01, "Another", "Another desc", 20.0,
            MAINTAINER_USERNAME));
  }

  @Test
  void createParameterShouldFailWhenUserIsNotMaintainer() throws WeatherReportException {
    facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);

    Assertions.assertThrows(
        UnauthorizedException.class,
        () -> facade.gateways().createParameter(GW_0101, PARAMETER_P01, "Param 1", "Desc 1", 10.0, VIEWER_USERNAME));
  }

  @Test
  void createParameterWithSameCodeInDifferentGatewaysShouldBeAllowed() throws WeatherReportException {
    facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);
    facade.gateways().createGateway(GW_0102, gatewayName("2"), desc("2"), MAINTAINER_USERNAME);

    Parameter p1 = facade.gateways().createParameter(GW_0101, PARAMETER_P01, "Param 1", "Desc 1", 10.0,
        MAINTAINER_USERNAME);
    Parameter p2 = facade.gateways().createParameter(GW_0102, PARAMETER_P01, "Param 1", "Desc 1", 20.0,
        MAINTAINER_USERNAME);

    Assertions.assertEquals(PARAMETER_P01, p1.getCode());
    Assertions.assertEquals(PARAMETER_P01, p2.getCode());
    Assertions.assertEquals(10.0, p1.getValue());
    Assertions.assertEquals(20.0, p2.getValue());
  }

  @Test
  void updateParameterShouldModifyValue() throws WeatherReportException {
    facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);
    facade.gateways().createParameter(GW_0101, PARAMETER_P01, "Param 1", "Desc 1", 10.0, MAINTAINER_USERNAME);

    Parameter updated = facade.gateways().updateParameter(GW_0101, PARAMETER_P01, 25.0, MAINTAINER_USERNAME);

    Assertions.assertEquals(PARAMETER_P01, updated.getCode());
    Assertions.assertEquals(25.0, updated.getValue());
  }

  @Test
  void updateParameterShouldFailWhenGatewayCodeIsNull() {
    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.gateways().updateParameter(null, PARAMETER_P01, 10.0, MAINTAINER_USERNAME));
  }

  @Test
  void updateParameterShouldFailWhenParameterCodeIsNull() {
    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.gateways().updateParameter(GW_0101, null, 10.0, MAINTAINER_USERNAME));
  }

  @Test
  void updateParameterShouldFailWhenGatewayDoesNotExist() {
    Assertions.assertThrows(
        ElementNotFoundException.class,
        () -> facade.gateways().updateParameter(GW_UNKNOWN, PARAMETER_P01, 10.0, MAINTAINER_USERNAME));
  }

  @Test
  void updateParameterShouldFailWhenParameterDoesNotExist() throws WeatherReportException {
    facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);

    Assertions.assertThrows(
        ElementNotFoundException.class,
        () -> facade.gateways().updateParameter(GW_0101, "UNKNOWN", 10.0, MAINTAINER_USERNAME));
  }

  @Test
  void updateParameterShouldFailWhenUserIsNotMaintainer() throws WeatherReportException {
    facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);
    facade.gateways().createParameter(GW_0101, PARAMETER_P01, "Param 1", "Desc 1", 10.0, MAINTAINER_USERNAME);

    Assertions.assertThrows(
        UnauthorizedException.class,
        () -> facade.gateways().updateParameter(GW_0101, PARAMETER_P01, 25.0, VIEWER_USERNAME));
  }

  @Test
  void gatewayShouldTrackCreatorAndLastModifier() throws WeatherReportException {
    LocalDateTime beforeCreate = LocalDateTime.now();
    Gateway gateway = facade.gateways().createGateway(GW_4242, "Gateway meta", "Desc meta", MAINTAINER_USERNAME);
    LocalDateTime afterCreate = LocalDateTime.now();

    Assertions.assertEquals(MAINTAINER_USERNAME, gateway.getCreatedBy());
    assertTimestampWithinTolerance(gateway.getCreatedAt(), beforeCreate, afterCreate);
    Assertions.assertNull(gateway.getModifiedBy());
    Assertions.assertNull(gateway.getModifiedAt());

    LocalDateTime beforeFirstUpdate = LocalDateTime.now();
    Gateway firstUpdate = facade.gateways().updateGateway(GW_4242, UPDATED_ONCE, DESC_UPDATED_ONCE,
        UPDATER_USERNAME);
    LocalDateTime afterFirstUpdate = LocalDateTime.now();
    Assertions.assertEquals(MAINTAINER_USERNAME, firstUpdate.getCreatedBy());
    Assertions.assertEquals(UPDATER_USERNAME, firstUpdate.getModifiedBy());
    assertTimestampWithinTolerance(firstUpdate.getModifiedAt(), beforeFirstUpdate, afterFirstUpdate);

    LocalDateTime beforeSecondUpdate = LocalDateTime.now();
    Gateway secondUpdate = facade.gateways().updateGateway(GW_4242, UPDATED_TWICE, DESC_UPDATED_TWICE,
        MAINTAINER_USERNAME);
    LocalDateTime afterSecondUpdate = LocalDateTime.now();
    Assertions.assertEquals(MAINTAINER_USERNAME, secondUpdate.getCreatedBy());
    Assertions.assertEquals(MAINTAINER_USERNAME, secondUpdate.getModifiedBy());
    assertTimestampWithinTolerance(secondUpdate.getModifiedAt(), beforeSecondUpdate, afterSecondUpdate);
    Assertions.assertTrue(
        !secondUpdate.getModifiedAt().isBefore(firstUpdate.getModifiedAt().minusSeconds(TIME_TOLERANCE_SECONDS)));
  }

  @Test
  void getGatewayReportShouldFailWhenGatewayDoesNotExist() {
    Assertions.assertThrows(
        ElementNotFoundException.class,
        () -> facade.gateways().getGatewayReport(GW_UNKNOWN, null, null));
  }

  @Test
  void getGatewayReportShouldFailWhenCodeIsNull() {
    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.gateways().getGatewayReport(null, null, null));
  }

  @Test
  void getGatewayReportWithoutMeasurementsShouldReturnEmptyStatsAndBatteryCharge() throws WeatherReportException {
    facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);
    facade.gateways().createParameter(GW_0101, Parameter.BATTERY_CHARGE_PERCENTAGE_CODE, "Battery", "Battery charge",
        75.0,
        MAINTAINER_USERNAME);

    GatewayReport report = facade.gateways().getGatewayReport(GW_0101, null, null);

    Assertions.assertNotNull(report);
    Assertions.assertEquals(GW_0101, report.getCode());
    Assertions.assertNull(report.getStartDate());
    Assertions.assertNull(report.getEndDate());
    Assertions.assertEquals(0, report.getNumberOfMeasurements());
    Assertions.assertTrue(report.getMostActiveSensors().isEmpty());
    Assertions.assertTrue(report.getLeastActiveSensors().isEmpty());
    Assertions.assertTrue(report.getSensorsLoadRatio().isEmpty());
    Assertions.assertTrue(report.getOutlierSensors().isEmpty());
    Assertions.assertTrue(report.getHistogram().isEmpty());
    Assertions.assertEquals(75.0, report.getBatteryChargePercentage(), 0.001);

  }

  @Test
  void getGatewayReportWithoutDatesShouldReturnStatsAndBatteryCharge() throws WeatherReportException {
    try (@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
    MockedConstruction<CRUDRepository> mocked = mockConstruction(CRUDRepository.class, (mock, context) -> {
      if (context.arguments().size() == 1 && context.arguments().get(0) == Sensor.class) {
        when(mock.read()).thenReturn(Collections.emptyList());
      } else {
        CRUDRepository<Object, Object> realRepo = new CRUDRepository<>((Class<Object>) context.arguments().get(0));
        when(mock.read(anyString())).thenAnswer(inv -> realRepo.read(inv.getArgument(0)));
        when(mock.read()).thenAnswer(inv -> realRepo.read());
        when(mock.create(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> realRepo.create(inv.getArgument(0)));
        when(mock.update(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> realRepo.update(inv.getArgument(0)));
        when(mock.delete(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> realRepo.delete(inv.getArgument(0)));
      }
    })) {
      facade.importDataFromFile(getClass().getClassLoader().getResource("csv/S_111.csv").getPath());
    }

    facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);

    GatewayReport report = facade.gateways().getGatewayReport(GW_0101, null, null);

    Assertions.assertNotNull(report);
    Assertions.assertEquals(GW_0101, report.getCode());
    Assertions.assertNull(report.getStartDate());
    Assertions.assertNull(report.getEndDate());
    Assertions.assertEquals(166, report.getNumberOfMeasurements());
    Assertions.assertFalse(report.getMostActiveSensors().isEmpty());
    Assertions.assertFalse(report.getSensorsLoadRatio().isEmpty());
    Assertions.assertFalse(report.getHistogram().isEmpty());
  }

  @Test
  void getGatewayReportWithDatesShouldReturnStatsAndDates() throws WeatherReportException {
    try (@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
    MockedConstruction<CRUDRepository> mocked = mockConstruction(CRUDRepository.class, (mock, context) -> {
      if (context.arguments().size() == 1 && context.arguments().get(0) == Sensor.class) {
        when(mock.read()).thenReturn(Collections.emptyList());
      } else {
        CRUDRepository<Object, Object> realRepo = new CRUDRepository<>((Class<Object>) context.arguments().get(0));
        when(mock.read(anyString())).thenAnswer(inv -> realRepo.read(inv.getArgument(0)));
        when(mock.read()).thenAnswer(inv -> realRepo.read());
        when(mock.create(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> realRepo.create(inv.getArgument(0)));
        when(mock.update(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> realRepo.update(inv.getArgument(0)));
        when(mock.delete(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> realRepo.delete(inv.getArgument(0)));
      }
    })) {
      facade.importDataFromFile(getClass().getClassLoader().getResource("csv/S_111.csv").getPath());
    }

    facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);

    GatewayReport report = facade.gateways().getGatewayReport(GW_0101, "2025-11-16 10:00:00", "2025-11-16 20:00:00");

    Assertions.assertNotNull(report);
    Assertions.assertEquals(GW_0101, report.getCode());
    Assertions.assertEquals("2025-11-16 10:00:00", report.getStartDate());
    Assertions.assertEquals("2025-11-16 20:00:00", report.getEndDate());
    Assertions.assertEquals(11, report.getNumberOfMeasurements());
    Assertions.assertFalse(report.getMostActiveSensors().isEmpty());
    Assertions.assertFalse(report.getSensorsLoadRatio().isEmpty());
    Assertions.assertFalse(report.getHistogram().isEmpty());
  }

  @Test
  void getGatewayReportWithOneDateShouldReturnStatsAndOneDate() throws WeatherReportException {
    try (@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
    MockedConstruction<CRUDRepository> mocked = mockConstruction(CRUDRepository.class, (mock, context) -> {
      if (context.arguments().size() == 1 && context.arguments().get(0) == Sensor.class) {
        when(mock.read()).thenReturn(Collections.emptyList());
      } else {
        CRUDRepository<Object, Object> realRepo = new CRUDRepository<>((Class<Object>) context.arguments().get(0));
        when(mock.read(anyString())).thenAnswer(inv -> realRepo.read(inv.getArgument(0)));
        when(mock.read()).thenAnswer(inv -> realRepo.read());
        when(mock.create(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> realRepo.create(inv.getArgument(0)));
        when(mock.update(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> realRepo.update(inv.getArgument(0)));
        when(mock.delete(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> realRepo.delete(inv.getArgument(0)));
      }
    })) {
      facade.importDataFromFile(getClass().getClassLoader().getResource("csv/S_111.csv").getPath());
    }

    facade.gateways().createGateway(GW_0101, gatewayName("1"), desc("1"), MAINTAINER_USERNAME);

    GatewayReport report = facade.gateways().getGatewayReport(GW_0101, "2025-11-22 20:00:00", null);

    Assertions.assertNotNull(report);
    Assertions.assertEquals(GW_0101, report.getCode());
    Assertions.assertEquals("2025-11-22 20:00:00", report.getStartDate());
    Assertions.assertNull(report.getEndDate());
    Assertions.assertEquals(10, report.getNumberOfMeasurements());
    Assertions.assertFalse(report.getMostActiveSensors().isEmpty());
    Assertions.assertFalse(report.getSensorsLoadRatio().isEmpty());
    Assertions.assertFalse(report.getHistogram().isEmpty());

    report = facade.gateways().getGatewayReport(GW_0101, null, "2025-11-16 20:00:00");

    Assertions.assertNotNull(report);
    Assertions.assertEquals(GW_0101, report.getCode());
    Assertions.assertNull(report.getStartDate());
    Assertions.assertEquals("2025-11-16 20:00:00", report.getEndDate());
    Assertions.assertEquals(13, report.getNumberOfMeasurements());
    Assertions.assertFalse(report.getMostActiveSensors().isEmpty());
    Assertions.assertFalse(report.getSensorsLoadRatio().isEmpty());
    Assertions.assertFalse(report.getHistogram().isEmpty());
  }

}
