package com.weather.report.test.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import com.weather.report.model.entities.Measurement;
import com.weather.report.model.entities.Sensor;
import com.weather.report.repositories.CRUDRepository;
import com.weather.report.repositories.MeasurementRepository;
import com.weather.report.test.BasePersistenceTest;

public class Test_R0 extends BasePersistenceTest {

  @Test
  void testImportDataFromFile() {
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
      String path = getClass().getClassLoader().getResource("csv/S_111.csv").getPath();
      MeasurementRepository measurementRepository = new MeasurementRepository();
      Collection<Measurement> currentMeasurements;

      facade.importDataFromFile(path);
      currentMeasurements = measurementRepository.read();

      assertEquals(166, currentMeasurements.size(),
          "First import should store 166 Measurement items, not " + currentMeasurements.size());

      for (Measurement m : currentMeasurements) {
        measurementRepository.delete(m.getId());
      }

      currentMeasurements = measurementRepository.read();

      assertEquals(0, currentMeasurements.size(),
          "After deletion there should be 0 Measurement items, not " + currentMeasurements.size());

      path = getClass().getClassLoader().getResource("csv/S_131.csv").getPath();
      facade.importDataFromFile(path);
      currentMeasurements = measurementRepository.read();

      assertEquals(100, currentMeasurements.size(),
          "Second import should store 100 Measurement items, not " + currentMeasurements.size());

      path = getClass().getClassLoader().getResource("csv/S_125.csv").getPath();
      facade.importDataFromFile(path);
      currentMeasurements = measurementRepository.read();

      assertEquals(198, currentMeasurements.size(),
          "After third import there should be 198 Measurement items, not " + currentMeasurements.size());

      path = getClass().getClassLoader().getResource("csv/S_122.csv").getPath();
      facade.importDataFromFile(path);
      currentMeasurements = measurementRepository.read();

      assertEquals(286, currentMeasurements.size(),
          "After third import there should be 286 Measurement items, not " + currentMeasurements.size());
    }
  }
}
