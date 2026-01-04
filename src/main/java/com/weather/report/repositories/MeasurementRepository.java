package com.weather.report.repositories;

import com.weather.report.model.entities.Measurement;

public class MeasurementRepository extends CRUDRepository<Measurement, Long> {

  public MeasurementRepository() {
    super(Measurement.class);
  }

}
