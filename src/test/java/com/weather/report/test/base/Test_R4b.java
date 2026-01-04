package com.weather.report.test.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.IdAlreadyInUseException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.entities.Gateway;
import com.weather.report.model.entities.Sensor;
import com.weather.report.test.BasePersistenceTest;

public class Test_R4b extends BasePersistenceTest {

  @BeforeAll
  static void checkBranchForR4() {
    assumeRequirement(4);
  }

  @Test
  void connectSensorLinksSensorToGateway()
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    createNetwork(NET_01);
    createGateway(GW_0001);
    connectGateway(NET_01, GW_0001);
    createSensor(SENSOR_000001);

    Gateway updated = facade.topology().connectSensor(SENSOR_000001, GW_0001, MAINTAINER_USERNAME);
    Collection<Sensor> sensors = facade.topology().getGatewaySensors(GW_0001);

    assertEquals(GW_0001, updated.getCode());
    assertCodes(sensors, Sensor::getCode, SENSOR_000001);
  }

  @Test
  void connectSensorSupportsMultipleGateways()
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    createNetwork(NET_01);
    createNetwork(NET_02);
    createGateway(GW_0001);
    createGateway(GW_0002);
    connectGateway(NET_01, GW_0001);
    connectGateway(NET_02, GW_0002);
    createSensor(SENSOR_000001);
    createSensor(SENSOR_000002);
    createSensor(SENSOR_000003);

    connectSensor(SENSOR_000001, GW_0001);
    connectSensor(SENSOR_000002, GW_0001);
    connectSensor(SENSOR_000003, GW_0002);

    assertCodes(facade.topology().getGatewaySensors(GW_0001), Sensor::getCode, SENSOR_000001, SENSOR_000002);
    assertCodes(facade.topology().getGatewaySensors(GW_0002), Sensor::getCode, SENSOR_000003);
  }

  @Test
  void connectSensorThrowsForInvalidData()
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    createGateway(GW_0001);
    createSensor(SENSOR_000001);

    assertThrows(InvalidInputDataException.class,
        () -> facade.topology().connectSensor(null, GW_0001, MAINTAINER_USERNAME));
    assertThrows(InvalidInputDataException.class,
        () -> facade.topology().connectSensor(SENSOR_000001, null, MAINTAINER_USERNAME));
    assertThrows(ElementNotFoundException.class,
        () -> facade.topology().connectSensor(SENSOR_000002, GW_0001, MAINTAINER_USERNAME));
    assertThrows(ElementNotFoundException.class,
        () -> facade.topology().connectSensor(SENSOR_000001, GW_0002, MAINTAINER_USERNAME));
  }

  @Test
  void connectSensorRequiresMaintainer()
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    createGateway(GW_0001);
    createSensor(SENSOR_000001);

    assertThrows(UnauthorizedException.class,
        () -> facade.topology().connectSensor(SENSOR_000001, GW_0001, VIEWER_USERNAME));
  }

  @Test
  void disconnectSensorRemovesSensorFromGateway()
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    createNetwork(NET_01);
    createGateway(GW_0001);
    createGateway(GW_0002);
    connectGateway(NET_01, GW_0001);
    createSensor(SENSOR_000001);
    createSensor(SENSOR_000002);

    connectSensor(SENSOR_000001, GW_0001);
    connectSensor(SENSOR_000002, GW_0001);

    Gateway updated = facade.topology().disconnectSensor(SENSOR_000001, GW_0001, MAINTAINER_USERNAME);

    assertEquals(GW_0001, updated.getCode());
    assertCodes(facade.topology().getGatewaySensors(GW_0001), Sensor::getCode, SENSOR_000002);
    assertTrue(facade.topology().getGatewaySensors(GW_0002).isEmpty());
  }

  @Test
  void disconnectSensorThrowsForInvalidData()
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    createGateway(GW_0001);
    createSensor(SENSOR_000001);
    connectSensor(SENSOR_000001, GW_0001);

    assertThrows(InvalidInputDataException.class,
        () -> facade.topology().disconnectSensor(null, GW_0001, MAINTAINER_USERNAME));
    assertThrows(InvalidInputDataException.class,
        () -> facade.topology().disconnectSensor(SENSOR_000001, null, MAINTAINER_USERNAME));
    assertThrows(ElementNotFoundException.class,
        () -> facade.topology().disconnectSensor(SENSOR_000001, GW_0002, MAINTAINER_USERNAME));
    assertThrows(ElementNotFoundException.class,
        () -> facade.topology().disconnectSensor(SENSOR_000002, GW_0001, MAINTAINER_USERNAME));
  }

  @Test
  void disconnectSensorRequiresMaintainer()
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    createGateway(GW_0001);
    createSensor(SENSOR_000001);
    connectSensor(SENSOR_000001, GW_0001);

    assertThrows(UnauthorizedException.class,
        () -> facade.topology().disconnectSensor(SENSOR_000001, GW_0001, VIEWER_USERNAME));
  }

  @Test
  void getGatewaySensorsReturnsSensorsForExistingGateway()
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    createGateway(GW_0003);
    createSensor(SENSOR_000001);
    createSensor(SENSOR_000002);

    connectSensor(SENSOR_000001, GW_0003);
    connectSensor(SENSOR_000002, GW_0003);

    Collection<Sensor> sensors = facade.topology().getGatewaySensors(GW_0003);

    assertCodes(sensors, Sensor::getCode, SENSOR_000001, SENSOR_000002);
  }

  @Test
  void getGatewaySensorsReturnsEmptyWhenNoSensors()
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    createGateway(GW_0004);

    Collection<Sensor> sensors = facade.topology().getGatewaySensors(GW_0004);

    assertNotNull(sensors);
    assertTrue(sensors.isEmpty());
  }

  @Test
  void getGatewaySensorsThrowsOnNullCode() {
    assertThrows(InvalidInputDataException.class, () -> facade.topology().getGatewaySensors(null));
  }

  @Test
  void getGatewaySensorsThrowsWhenGatewayNotFound() {
    assertThrows(ElementNotFoundException.class, () -> facade.topology().getGatewaySensors(GW_UNKNOWN));
  }

}
