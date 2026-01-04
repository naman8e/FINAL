package com.weather.report.model;

/**
 * User types within the weather reporting system.
 */
public enum UserType {
  /// user can only perform _read_ operations (consulting data and reports)
  VIEWER,
  /// user can perform both _read_ and _write_ operations (creation, update and deletion of entities and configurations).
  MAINTAINER
}
