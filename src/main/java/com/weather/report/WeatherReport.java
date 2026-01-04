package com.weather.report;

import com.weather.report.model.UserType;
import com.weather.report.model.entities.User;
import com.weather.report.operations.GatewayOperations;
import com.weather.report.operations.NetworkOperations;
import com.weather.report.operations.OperationsFactory;
import com.weather.report.operations.SensorOperations;
import com.weather.report.operations.TopologyOperations;
import com.weather.report.repositories.CRUDRepository;
import com.weather.report.services.DataImportingService;

public class WeatherReport {
  public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

  private final NetworkOperations networks = OperationsFactory.getNetworkOperations();
  private final GatewayOperations gateways = OperationsFactory.getGatewayOperations();
  private final SensorOperations sensors = OperationsFactory.getSensorOperations();
  private final TopologyOperations topology = OperationsFactory.getTopologyOperations();

  /*********************************
   ****** COMMON REQUIREMENTS ******
   *********************************/
  /**
   * Imports weather measurements from the given file into the system.
   * 
   * @param filePath the path of the file
   */
  public void importDataFromFile(String filePath) {
    DataImportingService.storeMeasurements(filePath);
  }

  /**
   * Creates a new user in the system.
   * 
   * @param username name of the user
   * @param type     type of user, either {@link UserType#VIEWER} or
   *                 {@link UserType#MAINTAINER}
   * @return the newly created user
   */
  public User createUser(String username, UserType type) {
    return new CRUDRepository<>(User.class).create(new User(username, type));
  }

  /*********************************
   ********* REQUIREMENTS **********
   *********************************/
  public NetworkOperations networks() {
    return networks;
  }

  public GatewayOperations gateways() {
    return gateways;
  }

  public SensorOperations sensors() {
    return sensors;
  }

  /*********************************
   ********* INTEGRATION **********
   *********************************/
  public TopologyOperations topology() {
    return topology;
  }
}
