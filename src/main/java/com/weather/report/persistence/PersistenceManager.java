package com.weather.report.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class PersistenceManager {
  private static final String TEST_PU_NAME = "weatherReportTestPU";
  private static final String PU_NAME = "weatherReportPU";

  private static EntityManagerFactory factory;
  private static String currentPUName = PersistenceManager.PU_NAME;

  public static void setTestMode() {
    if (factory != null && factory.isOpen()) {
      factory.close();
      factory = null;
    }
    currentPUName = PersistenceManager.TEST_PU_NAME;
  }

  private static EntityManagerFactory getCurrentFactory() {
    if (factory == null || !factory.isOpen()) {
      factory = Persistence.createEntityManagerFactory(currentPUName);
    }
    return factory;
  }

  public static EntityManager getEntityManager() {
    return getCurrentFactory().createEntityManager();
  }

  public static void close() {
    if (factory != null && factory.isOpen()) {
      factory.close();
    }
  }
}
