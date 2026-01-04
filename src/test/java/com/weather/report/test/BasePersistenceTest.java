package com.weather.report.test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import com.weather.report.WeatherReport;
import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.IdAlreadyInUseException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.UserType;
import com.weather.report.model.entities.Gateway;
import com.weather.report.model.entities.Network;
import com.weather.report.model.entities.Sensor;
import com.weather.report.persistence.PersistenceManager;

public abstract class BasePersistenceTest {
  private static final Pattern REQUIREMENT_BRANCH_PATTERN = Pattern.compile("^(\\d+)-r(\\d+)(?:-.+)?$");

  public static final String MAINTAINER_USERNAME = "maintainer";
  public static final String UPDATER_USERNAME = "updater";
  public static final String VIEWER_USERNAME = "viewer";

  public static final long TIME_TOLERANCE_SECONDS = 2L;

  protected static final String NET_01 = "NET_01";
  protected static final String NET_02 = "NET_02";
  protected static final String NET_03 = "NET_03";
  protected static final String NET_42 = "NET_42";
  protected static final String NET_99 = "NET_99";

  protected static final String GW_0001 = "GW_0001";
  protected static final String GW_0002 = "GW_0002";
  protected static final String GW_0003 = "GW_0003";
  protected static final String GW_0004 = "GW_0004";
  protected static final String GW_0101 = "GW_0101";
  protected static final String GW_0102 = "GW_0102";
  protected static final String GW_4242 = "GW_4242";
  protected static final String GW_UNKNOWN = "GW_9999";

  protected static final String SENSOR_000001 = "S_000001";
  protected static final String SENSOR_000002 = "S_000002";
  protected static final String SENSOR_000003 = "S_000003";
  protected static final String SENSOR_010101 = "S_010101";
  protected static final String SENSOR_010102 = "S_010102";
  protected static final String SENSOR_424242 = "S_424242";
  protected static final String SENSOR_UNKNOWN = "S_999999";

  protected static final String PARAMETER_P01 = "P_01";

  protected static final String OPERATOR_ALICE_FIRST = "Alice";
  protected static final String OPERATOR_ALICE_LAST = "Smith";
  protected static final String OPERATOR_ALICE_EMAIL = "alice@example.com";
  protected static final String OPERATOR_ALICE_PHONE = "123456789";
  protected static final String OPERATOR_BOB_EMAIL = "bob@example.com";

  protected static final String TEST_NETWORK_DESCRIPTION = "Test network";
  protected static final String UPDATED_NAME = "Updated name";
  protected static final String UPDATED_DESCRIPTION = "Updated description";
  protected static final String UPDATED_DESC = "Updated desc";
  protected static final String UPDATED_ONCE = "Updated once";
  protected static final String UPDATED_TWICE = "Updated twice";
  protected static final String DESC_UPDATED_ONCE = "Desc updated once";
  protected static final String DESC_UPDATED_TWICE = "Desc updated twice";

  private static String currentBranch;

  protected WeatherReport facade;

  static {
    currentBranch = System.getenv("CI_COMMIT_REF_NAME");
    if (currentBranch == null) {
      try {
        String head = Files.readString(Path.of(".git/HEAD"));
        if (head.startsWith("ref:")) {
          currentBranch = head.substring(head.lastIndexOf("/") + 1).trim();
        } else {
          currentBranch = "main";
        }
      } catch (IOException e) {
        currentBranch = "main";
      }
    }
  }

  public static void assumeRequirement(int reqNo) {
    if ("main".equals(currentBranch)) {
      return;
    }

    Matcher matcher = REQUIREMENT_BRANCH_PATTERN.matcher(currentBranch);

    if (!matcher.matches()) {
      return;
    }

    int branchRequirement = Integer.parseInt(matcher.group(2));

    if (branchRequirement >= 1 && branchRequirement <= 3) {
      boolean shouldRun = branchRequirement == reqNo;
      assumeTrue(
          shouldRun,
          "Skipping since branch " + currentBranch + " does not match individual requirement r" + reqNo);
    }
  }

  @BeforeEach
  protected void baseSetUp() {
    PersistenceManager.setTestMode();
    facade = new WeatherReport();
    facade.createUser(MAINTAINER_USERNAME, UserType.MAINTAINER);
    facade.createUser(UPDATER_USERNAME, UserType.MAINTAINER);
    facade.createUser(VIEWER_USERNAME, UserType.VIEWER);
  }

  @AfterEach
  void baseTearDown() {
    PersistenceManager.close();
  }

  public static void assertTimestampWithinTolerance(LocalDateTime timestamp, LocalDateTime earliest,
      LocalDateTime latest) {
    Assertions.assertNotNull(timestamp);
    Assertions.assertTrue(
        !timestamp.isBefore(earliest.minusSeconds(TIME_TOLERANCE_SECONDS)),
        "Timestamp is earlier than expected tolerance window");
    Assertions.assertTrue(
        !timestamp.isAfter(latest.plusSeconds(TIME_TOLERANCE_SECONDS)),
        "Timestamp is later than expected tolerance window");
  }

  protected String networkName(String suffix) {
    return "Network " + suffix;
  }

  protected String gatewayName(String suffix) {
    return "Gateway " + suffix;
  }

  protected String sensorName(String suffix) {
    return "Sensor " + suffix;
  }

  protected String description(String suffix) {
    return "Description " + suffix;
  }

  protected String desc(String suffix) {
    return "Desc " + suffix;
  }

  protected <T> void assertCodes(Collection<T> items, Function<T, String> extractor, String... expected) {
    Set<String> codes = items.stream().map(extractor).collect(Collectors.toSet());
    Assertions.assertEquals(expected.length, codes.size());
    for (String code : expected) {
      Assertions.assertTrue(codes.contains(code), "Missing code " + code);
    }
  }

  protected Network createNetwork(String code) throws InvalidInputDataException, IdAlreadyInUseException,
      UnauthorizedException {
    return facade.networks().createNetwork(code, networkName(code), description(code), MAINTAINER_USERNAME);
  }

  protected Gateway createGateway(String code)
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    return facade.gateways().createGateway(code, gatewayName(code), description(code), MAINTAINER_USERNAME);
  }

  protected Sensor createSensor(String code)
      throws InvalidInputDataException, IdAlreadyInUseException, ElementNotFoundException, UnauthorizedException {
    return facade.sensors().createSensor(code, sensorName(code), description(code), MAINTAINER_USERNAME);
  }

  protected Network connectGateway(String networkCode, String gatewayCode)
      throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException {
    return facade.topology().connectGateway(networkCode, gatewayCode, MAINTAINER_USERNAME);
  }

  protected Gateway connectSensor(String sensorCode, String gatewayCode)
      throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException {
    return facade.topology().connectSensor(sensorCode, gatewayCode, MAINTAINER_USERNAME);
  }

}
