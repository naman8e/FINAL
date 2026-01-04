package com.weather.report.services;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.weather.report.model.entities.Operator;

/**
 * Service handling user notifications (email/SMS) for threshold violations and
 * element
 * deletions, as required by the README.
 */
public class AlertingService {

  private static final Logger logger = LogManager.getLogger(AlertingService.class);

  /**
   * Notifies operators when a measurement exceeds a sensor threshold.
   *
   * @param operators  operators to alert
   * @param sensorCode code of the sensor that triggered the alert
   */
  public static void notifyThresholdViolation(Collection<Operator> operators, String sensorCode) {
    StringBuilder builder = new StringBuilder().append("Measured a value out of threshold bounds for sensor ")
        .append(sensorCode).append(", alerting operators");

    logger.warn(builder);

    for (Operator operator : operators) {
      sendEmail(operator);
      if (operator.getPhoneNumber() != null) {
        sendSMS(operator);
      }
    }
  }

  /**
   * Notifies the deletion of a Network, Gateway or Sensor.
   *
   * @param username     user performing the deletion
   * @param code         code of the deleted element
   * @param elementClass class of the deleted element to identify its type
   */
  public static void notifyDeletion(String username, String code, Class<?> elementClass) {
    logger.info(String.format("USer %s deleted %s %s", username, code, elementClass.getSimpleName()));
  }

  private static void sendEmail(Operator operator) {
    StringBuilder builder = new StringBuilder().append("Sending email to ").append(operator.getEmail()).append("\n");
    logger.info(builder);
  }

  private static void sendSMS(Operator operator) {
    StringBuilder builder = new StringBuilder().append("Sending SMS to ").append(operator.getEmail()).append("\n");
    logger.info(builder);
  }

}
