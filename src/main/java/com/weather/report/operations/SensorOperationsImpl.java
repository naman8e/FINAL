package com.weather.report.operations;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.weather.report.WeatherReport;
import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.IdAlreadyInUseException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.ThresholdType;
import com.weather.report.model.UserType;
import com.weather.report.model.entities.Measurement;
import com.weather.report.model.entities.Sensor;
import com.weather.report.model.entities.Threshold;
import com.weather.report.model.entities.User;
import com.weather.report.persistence.PersistenceManager;
import com.weather.report.reports.Report;
import com.weather.report.reports.SensorReport;
import com.weather.report.reports.SensorReportImpl;
import com.weather.report.services.AlertingService;
import com.weather.report.utils.ValidationUtils;

import jakarta.persistence.EntityManager;

// R3 implementation of SensorOperations
public class SensorOperationsImpl implements SensorOperations {

  // date formatter used by reports
  private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern(WeatherReport.DATE_FORMAT);

  /*
   * helper : mandatory enums must be != null
   * Threshold is mandatory cause we need it to compare
   */
  private static void validateThresholdType(ThresholdType type) throws InvalidInputDataException {
    if (type == null) {
      throw new InvalidInputDataException("type is mandatory");
    }
  }

  // helper : parses a report date in the required format (it can be null)
  private static LocalDateTime parseReportDate(String s) throws InvalidInputDataException {
    if (s == null)
      return null;
    try {
      return LocalDateTime.parse(s, REPORT_DATE_FORMATTER);
    } catch (RuntimeException e) {
      throw new InvalidInputDataException("Invalid date format");
    }
  }

  // helper : authorization check. Maintainer can; viewer, null or blank
  // throws an exception
  private static void requireMaintainer(EntityManager em, String username) throws UnauthorizedException {
    if (username == null || username.trim().isEmpty()) {
      throw new UnauthorizedException("Missing username");
    }

    User user = em.find(User.class, username);
    if (user == null) {
      throw new UnauthorizedException("user not authorized");
    }
    if (user.getType() != UserType.MAINTAINER) {
      throw new UnauthorizedException("user not authorized");
    }

  }

  @Override
  public Sensor createSensor(String code, String name, String description, String username)
      throws IdAlreadyInUseException, InvalidInputDataException, UnauthorizedException {

    ValidationUtils.validateSensorCode(code); // helper N.2

    EntityManager em = PersistenceManager.getEntityManager();
    try {
      // start a transaction to make sure that changes to the database are saved
      // correctly by JPA
      em.getTransaction().begin();

      requireMaintainer(em, username); 

      // verify and throw an exception if it does already exists a sensor with this
      // code
      Sensor s = em.find(Sensor.class, code);
      if (s != null) {
        throw new IdAlreadyInUseException("Sensor code already in use");
      }

      Sensor sensor = new Sensor(code, name, description);
      sensor.setCreatedBy(username);
      sensor.setCreatedAt(LocalDateTime.now());

      em.persist(sensor); // save the entity
      em.getTransaction().commit(); // confirm the transaction and saves the data to the database
      return sensor;

    } catch (RuntimeException ex) {
      if (em.getTransaction().isActive()) {
        em.getTransaction().rollback();
      } // eventually close the transaction before the exception
      throw ex;
    } finally {
      em.close();
    }
  }

  @Override
  public Sensor updateSensor(String code, String name, String description, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {

    ValidationUtils.validateSensorCode(code);

    EntityManager em = PersistenceManager.getEntityManager();
    try {
      em.getTransaction().begin();

      Sensor s = em.find(Sensor.class, code);
      if (s == null) {
        throw new ElementNotFoundException("Sensor not found");
      }

      requireMaintainer(em, username);

      s.setName(name);
      s.setDescription(description);

      s.setModifiedBy(username);
      s.setModifiedAt(LocalDateTime.now());

      em.getTransaction().commit();
      return s;

    } catch (RuntimeException ex) {
      if (em.getTransaction().isActive()) {
        em.getTransaction().rollback();
      }
      throw ex;
    } finally {
      em.close();
    }
  }

  @Override
  public Sensor deleteSensor(String code, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {

    ValidationUtils.validateSensorCode(code);

    EntityManager em = PersistenceManager.getEntityManager();
    try {
      em.getTransaction().begin();

      requireMaintainer(em, username);

      Sensor s = em.find(Sensor.class, code);
      if (s == null) {
        throw new ElementNotFoundException("Sensor not found");
      }

      // remove threshold if it is present
      Threshold t = em.find(Threshold.class, code);
      if (t != null) {
        em.remove(t);
      }

      em.remove(s); // remove sensor
      em.getTransaction().commit();

      // deletion notification
      AlertingService.notifyDeletion(username, code, Sensor.class);
      return s;

    } catch (RuntimeException ex) {
      if (em.getTransaction().isActive()) {
        em.getTransaction().rollback();
      }
      throw ex;
    } finally {
      em.close();
    }
  }

  @Override
  public Threshold createThreshold(String sensorCode, ThresholdType type, double value, String username)
      throws InvalidInputDataException, ElementNotFoundException, IdAlreadyInUseException, UnauthorizedException {

    ValidationUtils.validateSensorCode(sensorCode);
    validateThresholdType(type);

    EntityManager em = PersistenceManager.getEntityManager();
    try {
      em.getTransaction().begin();

      // sensor must exist
      Sensor sensor = em.find(Sensor.class, sensorCode);
      if (sensor == null) {
        throw new ElementNotFoundException("Sensor not found");
      }

      requireMaintainer(em, username);

      // only one threshold per sensor
      Threshold existing = em.find(Threshold.class, sensorCode);
      if (existing != null) {
        throw new IdAlreadyInUseException("Threshold already exists");
      }

      Threshold t = new Threshold(sensorCode, type, value);
      em.persist(t);

      // associate threshold to sensor
      sensor.setThreshold(t);

      em.getTransaction().commit();
      return t;

    } catch (RuntimeException ex) {
      if (em.getTransaction().isActive()) {
        em.getTransaction().rollback();
      }
      throw ex;
    } finally {
      em.close();
    }
  }

  @Override
  public Threshold updateThreshold(String sensorCode, ThresholdType type, double value, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {

    ValidationUtils.validateSensorCode(sensorCode);
    validateThresholdType(type);

    EntityManager em = PersistenceManager.getEntityManager();
    try {
      em.getTransaction().begin();

      Sensor sensor = em.find(Sensor.class, sensorCode);
      if (sensor == null) {
        throw new ElementNotFoundException("Sensor not found");
      }

      requireMaintainer(em, username);

      // threshold must exist
      Threshold t = em.find(Threshold.class, sensorCode);
      if (t == null) {
        throw new ElementNotFoundException("Threshold not found");
      }

      t.setType(type);
      t.setValue(value);

      // keep the relation coherent after updating the fields
      sensor.setThreshold(t);

      em.getTransaction().commit();
      return t;

    } catch (RuntimeException ex) {
      if (em.getTransaction().isActive()) {
        em.getTransaction().rollback();
      }
      throw ex;
    } finally {
      em.close();
    }
  }

  @Override
  public Collection<Sensor> getSensors(String... codes) {
    EntityManager em = PersistenceManager.getEntityManager();
    try {
      // no codes provided -> returns all sensors
      if (codes == null || codes.length == 0) {
        return em.createQuery("SELECT s FROM Sensor s", Sensor.class).getResultList();
      }

      // returns only existing sensors, no exceptions, no duplicates
      Map<String, Sensor> found = new LinkedHashMap<>(); // keeps insertion order and avoids duplicates
      for (String code : codes) {
        if (code == null || code.trim().isEmpty())
          continue;
        Sensor sensor = em.find(Sensor.class, code);
        if (sensor != null) {
          found.put(code, sensor);
        }
      }
      return new ArrayList<>(found.values());
    } finally {
      em.close();
    }
  }

  @Override
  public SensorReport getSensorReport(String code, String startDate, String endDate)
      throws InvalidInputDataException, ElementNotFoundException {

    ValidationUtils.validateSensorCode(code);

    LocalDateTime start = parseReportDate(startDate);
    LocalDateTime end = parseReportDate(endDate);

    if (start != null && end != null && start.isAfter(end)) {
      throw new InvalidInputDataException("Invalid interval");
    }

    EntityManager em = PersistenceManager.getEntityManager();
    try {
      // sensor must exist
      Sensor s = em.find(Sensor.class, code);
      if (s == null) {
        throw new ElementNotFoundException("Sensor not found");
      }

      List<Measurement> ms = loadSensorMeasurements(em, code, start, end);
      long n = ms.size();

      // no measurements -> empty report
      if (n == 0) {
        return new SensorReportImpl(
            code, startDate, endDate, 0,
            0.0, 0.0, 0.0,
            0.0, 0.0,
            List.of(),
            new TreeMap<>());
      }

      // basic stats
      double sum = 0.0;
      double min = Double.POSITIVE_INFINITY;
      double max = Double.NEGATIVE_INFINITY;
      List<Double> values = new ArrayList<>();

      for (Measurement m : ms) {
        double v = m.getValue();
        values.add(v);
        sum += v;
        min = Math.min(min, v);
        max = Math.max(max, v);
      }

      double mean = (n >= 1) ? sum / n : 0.0;

      final double meanFinal = mean;

      double variance = 0.0;
      double stdDev = 0.0;
      if (n >= 2) {
        variance = values.stream()
            .mapToDouble(v -> (v - meanFinal) * (v - meanFinal))
            .sum() / (n - 1); // sample variance
        stdDev = Math.sqrt(variance);
      }

      final double stdDevFinal = stdDev;

      // detect outliers: values outside +- 2 * stdDev from the mean
      List<Measurement> outliers = (n >= 2 && stdDev > 0.0)
          ? ms.stream()
              .filter(m -> Math.abs(m.getValue() - meanFinal) >= 2 * stdDevFinal)
              .collect(Collectors.toList())
          : List.of();

      // histogram compute on non-outliers only
      Set<Measurement> outSet = new HashSet<>(outliers);
      List<Double> nonOutValues = ms.stream()
          .filter(m -> !outSet.contains(m))
          .map(Measurement::getValue)
          .collect(Collectors.toList());

      SortedMap<Report.Range<Double>, Long> histogram = buildHistogram(nonOutValues);

      return new SensorReportImpl(
          code, startDate, endDate, n,
          mean, variance, stdDev,
          min, max,
          List.copyOf(outliers),
          histogram);

    } finally {
      em.close();
    }
  }

  // helper for histogram : loads sensor measurements with optional inclusive
  // bounds
  private static List<Measurement> loadSensorMeasurements(
      EntityManager em, String sensorCode, LocalDateTime start, LocalDateTime end) {

    StringBuilder jpql = new StringBuilder("SELECT m FROM Measurement m WHERE m.sensorCode = :code");

    if (start != null) {
      jpql.append(" AND m.timestamp >= :start");
    }
    if (end != null) {
      jpql.append(" AND m.timestamp <= :end");
    }

    jpql.append(" ORDER BY m.timestamp ASC");

    var q = em.createQuery(jpql.toString(), Measurement.class);
    q.setParameter("code", sensorCode);

    if (start != null) {
      q.setParameter("start", start);
    }
    if (end != null) {
      q.setParameter("end", end);
    }

    return q.getResultList();
  }

  /**
   * builds a histogram with 20 buckets over the given values
   * ranges are contiguous
   * the last bucket includes the maximum value
   */
  private static SortedMap<Report.Range<Double>, Long> buildHistogram(List<Double> values) {

    SortedMap<Report.Range<Double>, Long> histogramByRange = new TreeMap<>(
        (a, b) -> Double.compare(a.getStart(), b.getStart()));

    if (values == null || values.isEmpty())
      return histogramByRange;

    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    for (double v : values) {
      if (v < min)
        min = v;
      if (v > max)
        max = v;
    }

    if (min == max) {
      DoubleRange r = new DoubleRange(min, max, true);
      histogramByRange.put(r, (long) values.size());
      return histogramByRange;
    }

    final int BUCKETS = 20;
    double width = (max - min) / BUCKETS;

    List<DoubleRange> ranges = new ArrayList<>(BUCKETS);
    for (int i = 0; i < BUCKETS; i++) {
      double start = min + i * width;
      double end = (i == BUCKETS - 1) ? max : (min + (i + 1) * width);
      boolean includeEnd = (i == BUCKETS - 1);

      DoubleRange r = new DoubleRange(start, end, includeEnd);
      ranges.add(r);
      histogramByRange.put(r, 0L);
    }

    for (double v : values) {
      for (DoubleRange r : ranges) {
        if (r.contains(v)) {
          histogramByRange.put(r, histogramByRange.get(r) + 1L);
          break;
        }
      }
    }

    return histogramByRange;
  }

  // range implementation used as histogram key
  private static final class DoubleRange implements Report.Range<Double>, Comparable<DoubleRange> {

    private final double start;
    private final double end;
    private final boolean includeEnd;

    DoubleRange(double start, double end, boolean includeEnd) {
      this.start = start;
      this.end = end;
      this.includeEnd = includeEnd;
    }

    @Override
    public Double getStart() {
      return start;
    }

    @Override
    public Double getEnd() {
      return end;
    }

    @Override
    public boolean contains(Double value) {
      if (value == null)
        return false;
      if (value < start)
        return false;
      return includeEnd ? (value <= end) : (value < end);
    }

    @Override
    public int compareTo(DoubleRange other) {
      return Double.compare(this.start, other.start);
    }
  }

}