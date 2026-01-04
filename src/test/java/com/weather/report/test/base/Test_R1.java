package com.weather.report.test.base;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.IdAlreadyInUseException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.exceptions.WeatherReportException;
import com.weather.report.reports.NetworkReport;
import com.weather.report.model.ThresholdType;
import com.weather.report.model.entities.Network;
import com.weather.report.model.entities.Operator;
import com.weather.report.model.entities.Sensor;
import com.weather.report.model.entities.Threshold;
import com.weather.report.repositories.CRUDRepository;
import com.weather.report.services.AlertingService;
import com.weather.report.test.BasePersistenceTest;

public class Test_R1 extends BasePersistenceTest {

  @BeforeAll
  static void checkBranchForR1() {
    assumeRequirement(1);
  }

  @Test
  void createNetworkShouldCreateNetwork() throws WeatherReportException {
    try (MockedStatic<AlertingService> alerting = mockStatic(AlertingService.class)) {
      LocalDateTime beforeCreate = LocalDateTime.now();
      Network network = facade.networks().createNetwork(NET_01, networkName("1"), TEST_NETWORK_DESCRIPTION,
          MAINTAINER_USERNAME);
      LocalDateTime afterCreate = LocalDateTime.now();

      Assertions.assertNotNull(network);
      Assertions.assertEquals(NET_01, network.getCode());
      Assertions.assertEquals(networkName("1"), network.getName());
      Assertions.assertEquals(MAINTAINER_USERNAME, network.getCreatedBy());
      assertTimestampWithinTolerance(network.getCreatedAt(), beforeCreate, afterCreate);

      Collection<Network> networks = facade.networks().getNetworks(NET_01);
      Assertions.assertEquals(1, networks.size());
      Network loaded = networks.iterator().next();
      Assertions.assertEquals(NET_01, loaded.getCode());
      Assertions.assertEquals(networkName("1"), loaded.getName());
      Assertions.assertEquals(TEST_NETWORK_DESCRIPTION, loaded.getDescription());
      Assertions.assertEquals(MAINTAINER_USERNAME, loaded.getCreatedBy());
      assertTimestampWithinTolerance(loaded.getCreatedAt(), beforeCreate, afterCreate);

      alerting.verifyNoInteractions();
    }
  }

  @Test
  void createNetworkShouldFailWhenCodeIsNull() {
    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.networks().createNetwork(null, networkName("1"), TEST_NETWORK_DESCRIPTION, MAINTAINER_USERNAME));
  }

  @Test
  void createNetworkShouldFailWhenCodeAlreadyExists() throws WeatherReportException {
    facade.networks().createNetwork(NET_01, networkName("1"), TEST_NETWORK_DESCRIPTION, MAINTAINER_USERNAME);

    Assertions.assertThrows(
        IdAlreadyInUseException.class,
        () -> facade.networks().createNetwork(NET_01, "Another", "Another network", MAINTAINER_USERNAME));
  }

  @Test
  void createNetworkShouldFailWhenUserIsNotMaintainer() {
    Assertions.assertThrows(
        UnauthorizedException.class,
        () -> facade.networks().createNetwork(NET_01, networkName("1"), desc("1"), VIEWER_USERNAME));
  }

  @Test
  void updateNetworkShouldModifyExistingNetwork() throws WeatherReportException {
    try (MockedStatic<AlertingService> alerting = mockStatic(AlertingService.class)) {
      facade.networks().createNetwork(NET_01, networkName("1"), desc("1"), MAINTAINER_USERNAME);

      LocalDateTime beforeUpdate = LocalDateTime.now();
      Network updated = facade.networks().updateNetwork(NET_01, UPDATED_NAME, UPDATED_DESCRIPTION,
          UPDATER_USERNAME);
      LocalDateTime afterUpdate = LocalDateTime.now();

      Assertions.assertEquals(NET_01, updated.getCode());
      Assertions.assertEquals(UPDATED_NAME, updated.getName());
      Assertions.assertEquals(UPDATED_DESCRIPTION, updated.getDescription());
      Assertions.assertEquals(MAINTAINER_USERNAME, updated.getCreatedBy());
      Assertions.assertEquals(UPDATER_USERNAME, updated.getModifiedBy());
      assertTimestampWithinTolerance(updated.getModifiedAt(), beforeUpdate, afterUpdate);

      Collection<Network> networks = facade.networks().getNetworks(NET_01);
      Assertions.assertEquals(1, networks.size());
      Network reloaded = networks.iterator().next();
      Assertions.assertEquals(UPDATED_NAME, reloaded.getName());
      Assertions.assertEquals(UPDATED_DESCRIPTION, reloaded.getDescription());
      Assertions.assertEquals(MAINTAINER_USERNAME, reloaded.getCreatedBy());
      Assertions.assertEquals(UPDATER_USERNAME, reloaded.getModifiedBy());
      assertTimestampWithinTolerance(reloaded.getModifiedAt(), beforeUpdate, afterUpdate);

      alerting.verifyNoInteractions();
    }
  }

  @Test
  void updateNetworkShouldFailWhenCodeIsNull() {
    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.networks().updateNetwork(null, "Name", "Desc", MAINTAINER_USERNAME));
  }

  @Test
  void updateNetworkShouldFailWhenNetworkDoesNotExist() {
    Assertions.assertThrows(
        ElementNotFoundException.class,
        () -> facade.networks().updateNetwork(NET_99, "Name", "Desc", MAINTAINER_USERNAME));
  }

  @Test
  void updateNetworkShouldFailWhenUserIsNotMaintainer() throws WeatherReportException {
    facade.networks().createNetwork(NET_01, networkName("1"), desc("1"), MAINTAINER_USERNAME);

    Assertions.assertThrows(
        UnauthorizedException.class,
        () -> facade.networks().updateNetwork(NET_01, UPDATED_NAME, UPDATED_DESCRIPTION, VIEWER_USERNAME));
  }

  @Test
  void deleteNetworkShouldRemoveExistingNetwork() throws WeatherReportException {
    try (MockedStatic<AlertingService> alerting = mockStatic(AlertingService.class)) {
      facade.networks().createNetwork(NET_01, networkName("1"), desc("1"), MAINTAINER_USERNAME);

      Network deleted = facade.networks().deleteNetwork(NET_01, MAINTAINER_USERNAME);
      Assertions.assertEquals(NET_01, deleted.getCode());

      Collection<Network> networks = facade.networks().getNetworks(NET_01);
      Assertions.assertTrue(networks.isEmpty());

      alerting.verify(() -> AlertingService.notifyDeletion(MAINTAINER_USERNAME, NET_01, Network.class));
    }
  }

  @Test
  void deleteNetworkShouldFailWhenNetworkDoesNotExist() {
    Assertions.assertThrows(
        ElementNotFoundException.class,
        () -> facade.networks().deleteNetwork(NET_99, MAINTAINER_USERNAME));
  }

  @Test
  void deleteNetworkShouldFailWhenUserIsNotMaintainer() throws WeatherReportException {
    facade.networks().createNetwork(NET_01, networkName("1"), desc("1"), MAINTAINER_USERNAME);

    Assertions.assertThrows(
        UnauthorizedException.class,
        () -> facade.networks().deleteNetwork(NET_01, VIEWER_USERNAME));
  }

  @Test
  void getNetworksWithoutArgumentsShouldReturnAllNetworks() throws WeatherReportException {
    facade.networks().createNetwork(NET_01, networkName("1"), desc("1"), MAINTAINER_USERNAME);
    facade.networks().createNetwork(NET_02, networkName("2"), desc("2"), MAINTAINER_USERNAME);

    Collection<Network> networks = facade.networks().getNetworks();

    Assertions.assertEquals(2, networks.size());
  }

  @Test
  void createOperatorShouldCreateOperator() throws WeatherReportException {
    Operator operator = facade.networks().createOperator(
        OPERATOR_ALICE_FIRST,
        OPERATOR_ALICE_LAST,
        OPERATOR_ALICE_EMAIL,
        OPERATOR_ALICE_PHONE,
        MAINTAINER_USERNAME);

    Assertions.assertNotNull(operator);
    Assertions.assertEquals(OPERATOR_ALICE_EMAIL, operator.getEmail());
    Assertions.assertEquals(OPERATOR_ALICE_FIRST, operator.getFirstName());
    Assertions.assertEquals(OPERATOR_ALICE_LAST, operator.getLastName());
    Assertions.assertEquals(OPERATOR_ALICE_PHONE, operator.getPhoneNumber());
  }

  @Test
  void createOperatorShouldFailWhenMandatoryFieldIsNull() {
    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.networks().createOperator(null, OPERATOR_ALICE_LAST, OPERATOR_ALICE_EMAIL, "123",
            MAINTAINER_USERNAME));

    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.networks().createOperator(OPERATOR_ALICE_FIRST, null, OPERATOR_ALICE_EMAIL, "123",
            MAINTAINER_USERNAME));

    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.networks().createOperator(OPERATOR_ALICE_FIRST, OPERATOR_ALICE_LAST, null, "123",
            MAINTAINER_USERNAME));
  }

  @Test
  void createOperatorShouldFailWhenEmailAlreadyExists() throws WeatherReportException {
    facade.networks().createOperator(OPERATOR_ALICE_FIRST, OPERATOR_ALICE_LAST, OPERATOR_ALICE_EMAIL, "123",
        MAINTAINER_USERNAME);

    Assertions.assertThrows(
        IdAlreadyInUseException.class,
        () -> facade.networks().createOperator("Bob", "Brown", OPERATOR_ALICE_EMAIL, "456", MAINTAINER_USERNAME));
  }

  @Test
  void createOperatorShouldFailWhenUserIsNotMaintainer() {
    Assertions.assertThrows(
        UnauthorizedException.class,
        () -> facade.networks().createOperator(OPERATOR_ALICE_FIRST, OPERATOR_ALICE_LAST, OPERATOR_ALICE_EMAIL, "123",
            VIEWER_USERNAME));
  }

  @Test
  void addOperatorToNetworkShouldAssociateOperator() throws WeatherReportException {
    facade.networks().createNetwork(NET_01, networkName("1"), desc("1"), MAINTAINER_USERNAME);
    facade.networks().createOperator(OPERATOR_ALICE_FIRST, OPERATOR_ALICE_LAST, OPERATOR_ALICE_EMAIL, "123",
        MAINTAINER_USERNAME);

    Network network = facade.networks().addOperatorToNetwork(NET_01, OPERATOR_ALICE_EMAIL, MAINTAINER_USERNAME);

    Assertions.assertEquals(NET_01, network.getCode());
    Assertions.assertFalse(network.getOperators().isEmpty());
    Operator associated = network.getOperators().iterator().next();
    Assertions.assertEquals(OPERATOR_ALICE_EMAIL, associated.getEmail());
  }

  @Test
  void addOperatorToNetworkShouldFailWhenParametersAreNull() {
    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.networks().addOperatorToNetwork(null, OPERATOR_ALICE_EMAIL, MAINTAINER_USERNAME));

    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.networks().addOperatorToNetwork(NET_01, null, MAINTAINER_USERNAME));
  }

  @Test
  void addOperatorToNetworkShouldFailWhenNetworkDoesNotExist() throws WeatherReportException {
    facade.networks().createOperator(OPERATOR_ALICE_FIRST, OPERATOR_ALICE_LAST, OPERATOR_ALICE_EMAIL, "123",
        MAINTAINER_USERNAME);

    Assertions.assertThrows(
        ElementNotFoundException.class,
        () -> facade.networks().addOperatorToNetwork(NET_99, OPERATOR_ALICE_EMAIL, MAINTAINER_USERNAME));
  }

  @Test
  void addOperatorToNetworkShouldFailWhenOperatorDoesNotExist() throws WeatherReportException {
    facade.networks().createNetwork(NET_01, networkName("1"), desc("1"), MAINTAINER_USERNAME);

    Assertions.assertThrows(
        ElementNotFoundException.class,
        () -> facade.networks().addOperatorToNetwork(NET_01, "unknown@example.com", MAINTAINER_USERNAME));
  }

  @Test
  void addOperatorToNetworkShouldFailWhenUserIsNotMaintainer() throws WeatherReportException {
    facade.networks().createNetwork(NET_01, networkName("1"), desc("1"), MAINTAINER_USERNAME);
    facade.networks().createOperator(OPERATOR_ALICE_FIRST, OPERATOR_ALICE_LAST, OPERATOR_ALICE_EMAIL, "123",
        MAINTAINER_USERNAME);

    Assertions.assertThrows(
        UnauthorizedException.class,
        () -> facade.networks().addOperatorToNetwork(NET_01, OPERATOR_ALICE_EMAIL, VIEWER_USERNAME));
  }

  @Test
  void getNetworkReportShouldFailWhenNetworkDoesNotExist() {
    Assertions.assertThrows(
        ElementNotFoundException.class,
        () -> facade.networks().getNetworkReport(NET_99, null, null));
  }

  @Test
  void getNetworkReportShouldFailWhenCodeIsNull() {
    Assertions.assertThrows(
        InvalidInputDataException.class,
        () -> facade.networks().getNetworkReport(null, null, null));
  }

  @Test
  void getNetworkReportWithoutMeasurementsShouldReturnEmptyStats() throws WeatherReportException {
    facade.networks().createNetwork(NET_01, networkName("1"), desc("1"), MAINTAINER_USERNAME);

    NetworkReport report = facade.networks().getNetworkReport(NET_01, null, null);

    Assertions.assertNotNull(report);
    Assertions.assertEquals(NET_01, report.getCode());
    Assertions.assertNull(report.getStartDate());
    Assertions.assertNull(report.getEndDate());
    Assertions.assertEquals(0, report.getNumberOfMeasurements());
    Assertions.assertTrue(report.getMostActiveGateways().isEmpty());
    Assertions.assertTrue(report.getLeastActiveGateways().isEmpty());
    Assertions.assertTrue(report.getGatewaysLoadRatio().isEmpty());
    Assertions.assertTrue(report.getHistogram().isEmpty());
  }

  @Test
  void getNetworkReportWithoutDatesShouldReturnStats() throws WeatherReportException {
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

    facade.networks().createNetwork(NET_01, networkName("1"), desc("1"), MAINTAINER_USERNAME);

    NetworkReport report = facade.networks().getNetworkReport(NET_01, null, null);

    Assertions.assertNotNull(report);
    Assertions.assertEquals(NET_01, report.getCode());
    Assertions.assertNull(report.getStartDate());
    Assertions.assertNull(report.getEndDate());
    Assertions.assertEquals(166, report.getNumberOfMeasurements());
    Assertions.assertFalse(report.getMostActiveGateways().isEmpty());
    Assertions.assertFalse(report.getGatewaysLoadRatio().isEmpty());
    Assertions.assertFalse(report.getHistogram().isEmpty());
  }

  @Test
  void getNetworkReportWithDatesShouldReturnStatsAndDates() throws WeatherReportException {
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

    facade.networks().createNetwork(NET_01, networkName("1"), desc("1"), MAINTAINER_USERNAME);

    NetworkReport report = facade.networks().getNetworkReport(NET_01, "2025-11-16 10:00:00", "2025-11-16 20:00:00");

    Assertions.assertNotNull(report);
    Assertions.assertEquals(NET_01, report.getCode());
    Assertions.assertEquals("2025-11-16 10:00:00", report.getStartDate());
    Assertions.assertEquals("2025-11-16 20:00:00", report.getEndDate());
    Assertions.assertEquals(11, report.getNumberOfMeasurements());
    Assertions.assertFalse(report.getMostActiveGateways().isEmpty());
    Assertions.assertFalse(report.getGatewaysLoadRatio().isEmpty());
    Assertions.assertFalse(report.getHistogram().isEmpty());
  }

  @Test
  void getNetworkReportWithOneDateShouldReturnStatsAndOneDate() throws WeatherReportException {
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

    facade.networks().createNetwork(NET_01, networkName("1"), desc("1"), MAINTAINER_USERNAME);

    NetworkReport report = facade.networks().getNetworkReport(NET_01, null, "2025-11-16 20:00:00");

    Assertions.assertNotNull(report);
    Assertions.assertEquals(NET_01, report.getCode());
    Assertions.assertNull(report.getStartDate());
    Assertions.assertEquals("2025-11-16 20:00:00", report.getEndDate());
    Assertions.assertEquals(13, report.getNumberOfMeasurements());
    Assertions.assertFalse(report.getMostActiveGateways().isEmpty());
    Assertions.assertFalse(report.getGatewaysLoadRatio().isEmpty());
    Assertions.assertFalse(report.getHistogram().isEmpty());

    report = facade.networks().getNetworkReport(NET_01, "2025-11-22 20:00:00", null);

    Assertions.assertNotNull(report);
    Assertions.assertEquals(NET_01, report.getCode());
    Assertions.assertEquals("2025-11-22 20:00:00", report.getStartDate());
    Assertions.assertNull(report.getEndDate());
    Assertions.assertEquals(10, report.getNumberOfMeasurements());
    Assertions.assertFalse(report.getMostActiveGateways().isEmpty());
    Assertions.assertFalse(report.getGatewaysLoadRatio().isEmpty());
    Assertions.assertFalse(report.getHistogram().isEmpty());
  }

  @Test
  void importDataShouldTriggerThresholdNotification() throws Throwable {
    try (
        MockedStatic<AlertingService> alerting = mockStatic(AlertingService.class);
        @SuppressWarnings({ "rawtypes", "unchecked", "unused" })
        MockedConstruction<CRUDRepository> mockedRepo = mockConstruction(CRUDRepository.class, (mock, context) -> {
          if (context.arguments().size() == 1 && context.arguments().get(0) == Sensor.class) {
            Threshold threshold = mock(Threshold.class);
            when(threshold.getType()).thenReturn(ThresholdType.GREATER_THAN);
            when(threshold.getValue()).thenReturn(24.0);

            Sensor sensor = mock(Sensor.class);
            when(sensor.getCode()).thenReturn(SENSOR_010101);
            when(sensor.getName()).thenReturn(sensorName("1"));
            when(sensor.getThreshold()).thenReturn(threshold);
            when(mock.read()).thenReturn(List.of(sensor));
          } else {
            CRUDRepository<Object, Object> realRepo = new CRUDRepository<>((Class<Object>) context.arguments().get(0));
            when(mock.read(anyString())).thenAnswer(inv -> realRepo.read(inv.getArgument(0)));
            when(mock.read()).thenAnswer(inv -> realRepo.read());
            when(mock.create(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> realRepo.create(inv.getArgument(0)));
            when(mock.update(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> realRepo.update(inv.getArgument(0)));
            when(mock.delete(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> realRepo.delete(inv.getArgument(0)));
          }
        })) {
      facade.networks().createNetwork(NET_01, networkName("1"), desc("1"), MAINTAINER_USERNAME);
      facade.networks().createOperator(OPERATOR_ALICE_FIRST, OPERATOR_ALICE_LAST, OPERATOR_ALICE_EMAIL,
          OPERATOR_ALICE_PHONE, MAINTAINER_USERNAME);
      facade.networks().addOperatorToNetwork(NET_01, OPERATOR_ALICE_EMAIL, MAINTAINER_USERNAME);

      String path = getClass().getClassLoader().getResource("csv/S_111.csv").getPath();
      facade.importDataFromFile(path);

      alerting.verify(
          () -> AlertingService.notifyThresholdViolation(
              argThat(ops -> ops.stream().anyMatch(o -> OPERATOR_ALICE_EMAIL.equals(o.getEmail()))),
              eq(SENSOR_010101)));
      alerting.verifyNoMoreInteractions();
    }
  }

  @Test
  void networkShouldTrackCreatorAndLastModifier() throws WeatherReportException {
    LocalDateTime beforeCreate = LocalDateTime.now();
    Network network = facade.networks().createNetwork(NET_42, networkName("42"), desc("42"), MAINTAINER_USERNAME);
    LocalDateTime afterCreate = LocalDateTime.now();

    Assertions.assertEquals(MAINTAINER_USERNAME, network.getCreatedBy());
    assertTimestampWithinTolerance(network.getCreatedAt(), beforeCreate, afterCreate);

    LocalDateTime beforeFirstUpdate = LocalDateTime.now();
    Network firstUpdate = facade.networks().updateNetwork(NET_42, UPDATED_ONCE, DESC_UPDATED_ONCE,
        UPDATER_USERNAME);
    LocalDateTime afterFirstUpdate = LocalDateTime.now();

    Assertions.assertEquals(UPDATED_ONCE, firstUpdate.getName());
    Assertions.assertEquals(DESC_UPDATED_ONCE, firstUpdate.getDescription());
    Assertions.assertEquals(MAINTAINER_USERNAME, firstUpdate.getCreatedBy());
    Assertions.assertEquals(UPDATER_USERNAME, firstUpdate.getModifiedBy());
    assertTimestampWithinTolerance(firstUpdate.getModifiedAt(), beforeFirstUpdate, afterFirstUpdate);

    LocalDateTime beforeSecondUpdate = LocalDateTime.now();
    Network secondUpdate = facade.networks().updateNetwork(NET_42, UPDATED_TWICE, DESC_UPDATED_TWICE,
        MAINTAINER_USERNAME);
    LocalDateTime afterSecondUpdate = LocalDateTime.now();

    Assertions.assertEquals(UPDATED_TWICE, secondUpdate.getName());
    Assertions.assertEquals(DESC_UPDATED_TWICE, secondUpdate.getDescription());
    Assertions.assertEquals(MAINTAINER_USERNAME, secondUpdate.getCreatedBy());
    Assertions.assertEquals(MAINTAINER_USERNAME, secondUpdate.getModifiedBy());
    assertTimestampWithinTolerance(secondUpdate.getModifiedAt(), beforeSecondUpdate,
        afterSecondUpdate);
    Assertions.assertTrue(
        !secondUpdate.getModifiedAt()
            .isBefore(firstUpdate.getModifiedAt().minusSeconds(TIME_TOLERANCE_SECONDS)));
  }

}
