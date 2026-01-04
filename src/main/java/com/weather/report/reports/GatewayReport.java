package com.weather.report.reports;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

/**
 * Report at gateway level with the metrics defined for R2, in addition to the
 * common
 * details provided by {@link Report} (gateway code, date interval and total
 * measurements).
 * Values are computed on the requested time window, except for the battery
 * charge,
 * which reflects the current {@code BATTERY_CHARGE} parameter of the gateway.
 */
public interface GatewayReport extends Report<Duration> {

  /**
   * Sensors that produced the highest number of measurements for the gateway in
   * the
   * requested interval.
   *
   * @return collection of sensor codes ordered arbitrarily when multiple sensors
   *         share
   *         the maximum
   */
  public Collection<String> getMostActiveSensors();

  /**
   * Sensors that produced the lowest number of measurements for the gateway in
   * the
   * requested interval.
   *
   * @return collection of sensor codes ordered arbitrarily when multiple sensors
   *         share
   *         the minimum
   */
  public Collection<String> getLeastActiveSensors();

  /**
   * Percentage of the gateway measurements contributed by each sensor in the
   * requested
   * interval.
   *
   * @return map {@code <sensorCode, ratio>} where ratio is expressed as a
   *         percentage of
   *         the gateway total
   */
  public Map<String, Double> getSensorsLoadRatio();

  /**
   * Sensors whose mean measurement differs from the gateway expected mean
   * ({@code EXPECTED_MEAN}) by at least two times the expected standard deviation
   * ({@code EXPECTED_STD_DEV}) in the requested interval.
   *
   * @return collection of sensor codes flagged as outliers
   */
  public Collection<String> getOutlierSensors();

  /**
   * Current value of the gateway {@code BATTERY_CHARGE} parameter (independent of
   * the
   * requested interval).
   *
   * @return battery charge percentage
   */
  public double getBatteryChargePercentage();

  /**
   * Returns a histogram of the inter-arrival times between consecutive
   * measurements of this gateway within the requested interval.
   *
   * The method only considers measurements that belong to the current gateway
   * and that fall within an effective interval [effectiveStart, effectiveEnd],
   * defined as follows:
   *
   * - if startDate is non-null, effectiveStart is the parsed value of startDate;
   * otherwise, effectiveStart is the timestamp of the earliest measurement
   * available for this gateway (or null if no measurements exist);
   *
   * - if endDate is non-null, effectiveEnd is the parsed value of endDate;
   * otherwise, effectiveEnd is the timestamp of the latest measurement
   * available for this gateway (or null if no measurements exist).
   *
   * If the effective interval contains fewer than two measurements, the method
   * returns an empty map, because no inter-arrival time can be computed.
   *
   * When at least two measurements are present, the method computes the
   * inter-arrival times as the {@link java.time.Duration} between the timestamp
   * of each measurement and the timestamp of the next one in ascending
   * chronological order.
   *
   * Starting from the minimum and maximum inter-arrival times observed in the
   * effective interval, the method partitions the [minDuration, maxDuration]
   * range into 20 of contiguous buckets, each represented by a
   * {@code Range<Duration>}:
   *
   * - all buckets together fully cover the [minDuration, maxDuration] interval;
   * - buckets are non-overlapping and contiguous;
   * - each bucket is represented by a {@code Range<Duration>} that stores
   * its start and end bounds;
   * - the partitioning strategy (number and width of buckets) is chosen by the
   * implementation based on the data distribution and/or configuration.
   *
   * Each entry of the returned map represents a single duration bucket:
   *
   * - the key is a {@code Range<Duration>} describing an inter-arrival time
   * interval;
   *
   * - the value is the total number of observed inter-arrival times whose
   * duration falls into that bucket according to the histogram convention:
   * all buckets except the last one are left-closed and right-open
   * [start, end), while the last bucket is [start, end] so that the
   * maximum inter-arrival time is included.
   *
   * The returned map is a {@link java.util.SortedMap} ordered by ascending
   * bucket start duration.
   *
   * @return a sorted map where each key is a {@code Range<Duration>} describing
   *         a bucket of inter-arrival times and each value is the number of
   *         occurrences falling into that bucket
   */
  @Override
  public SortedMap<Range<Duration>, Long> getHistogram();

}
