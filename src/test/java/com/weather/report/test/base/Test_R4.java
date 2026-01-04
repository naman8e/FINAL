package com.weather.report.test.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.weather.report.model.entities.Network;
import com.weather.report.test.BasePersistenceTest;

public class Test_R4 extends BasePersistenceTest {

  @BeforeAll
  static void checkBranchForR4() {
    assumeRequirement(4);
  }

  @Test
  void connectGatewayLinksGatewayToNetwork()
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    createNetwork(NET_01);
    createGateway(GW_0001);

    Network updated = facade.topology().connectGateway(NET_01, GW_0001, MAINTAINER_USERNAME);
    Collection<Gateway> gateways = facade.topology().getNetworkGateways(NET_01);

    assertEquals(NET_01, updated.getCode());
    assertCodes(gateways, Gateway::getCode, GW_0001);
  }

  @Test
  void connectGatewayHandlesMultipleNetworks()
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    createNetwork(NET_01);
    createNetwork(NET_02);
    createGateway(GW_0001);
    createGateway(GW_0002);
    createGateway(GW_0003);
    createGateway(GW_0004);

    connectGateway(NET_01, GW_0001);
    connectGateway(NET_01, GW_0002);
    connectGateway(NET_02, GW_0003);

    assertCodes(facade.topology().getNetworkGateways(NET_01), Gateway::getCode, GW_0001, GW_0002);
    assertCodes(facade.topology().getNetworkGateways(NET_02), Gateway::getCode, GW_0003);
    assertTrue(facade.topology().getNetworkGateways(NET_02).stream()
        .map(Gateway::getCode)
        .noneMatch(GW_0004::equals));
  }

  @Test
  void connectGatewayThrowsForInvalidData()
      throws InvalidInputDataException, IdAlreadyInUseException, UnauthorizedException, ElementNotFoundException {
    createNetwork(NET_01);
    createGateway(GW_0001);

    assertThrows(InvalidInputDataException.class,
        () -> facade.topology().connectGateway(null, GW_0001, MAINTAINER_USERNAME));
    assertThrows(InvalidInputDataException.class,
        () -> facade.topology().connectGateway(NET_01, null, MAINTAINER_USERNAME));
    assertThrows(ElementNotFoundException.class,
        () -> facade.topology().connectGateway(NET_03, GW_0001, MAINTAINER_USERNAME));
    assertThrows(ElementNotFoundException.class,
        () -> facade.topology().connectGateway(NET_01, GW_0003, MAINTAINER_USERNAME));
  }

  @Test
  void connectGatewayRequiresMaintainer()
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    createNetwork(NET_01);
    createGateway(GW_0001);

    assertThrows(UnauthorizedException.class,
        () -> facade.topology().connectGateway(NET_01, GW_0001, VIEWER_USERNAME));
  }

  @Test
  void disconnectGatewayRemovesGatewayFromNetwork()
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    createNetwork(NET_01);
    createNetwork(NET_02);
    createGateway(GW_0001);
    createGateway(GW_0002);
    createGateway(GW_0003);

    connectGateway(NET_01, GW_0001);
    connectGateway(NET_01, GW_0002);
    connectGateway(NET_02, GW_0003);

    Network updated = facade.topology().disconnectGateway(NET_01, GW_0001, MAINTAINER_USERNAME);

    assertEquals(NET_01, updated.getCode());
    assertCodes(facade.topology().getNetworkGateways(NET_01), Gateway::getCode, GW_0002);
    assertCodes(facade.topology().getNetworkGateways(NET_02), Gateway::getCode, GW_0003);
  }

  @Test
  void disconnectGatewayThrowsForInvalidData()
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    createNetwork(NET_01);
    createGateway(GW_0001);
    connectGateway(NET_01, GW_0001);

    assertThrows(InvalidInputDataException.class,
        () -> facade.topology().disconnectGateway(null, GW_0001, MAINTAINER_USERNAME));
    assertThrows(InvalidInputDataException.class,
        () -> facade.topology().disconnectGateway(NET_01, null, MAINTAINER_USERNAME));
    assertThrows(ElementNotFoundException.class,
        () -> facade.topology().disconnectGateway(NET_03, GW_0001, MAINTAINER_USERNAME));
    assertThrows(ElementNotFoundException.class,
        () -> facade.topology().disconnectGateway(NET_01, GW_0002, MAINTAINER_USERNAME));
  }

  @Test
  void disconnectGatewayRequiresMaintainer()
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    createNetwork(NET_01);
    createGateway(GW_0001);
    connectGateway(NET_01, GW_0001);

    assertThrows(UnauthorizedException.class,
        () -> facade.topology().disconnectGateway(NET_01, GW_0001, VIEWER_USERNAME));
  }

  @Test
  void getNetworkGatewaysReturnsOnlyConnectedGateways()
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    createNetwork(NET_01);
    createNetwork(NET_02);
    createGateway(GW_0001);
    createGateway(GW_0002);
    createGateway(GW_0003);

    connectGateway(NET_01, GW_0001);
    connectGateway(NET_02, GW_0002);

    assertCodes(facade.topology().getNetworkGateways(NET_01), Gateway::getCode, GW_0001);
    assertCodes(facade.topology().getNetworkGateways(NET_02), Gateway::getCode, GW_0002);
  }

  @Test
  void getNetworkGatewaysThrowsOnNullCode() {
    assertThrows(InvalidInputDataException.class, () -> facade.topology().getNetworkGateways(null));
  }

  @Test
  void getNetworkGatewaysThrowsWhenNetworkNotFound() {
    assertThrows(ElementNotFoundException.class, () -> facade.topology().getNetworkGateways(NET_99));
  }

}
