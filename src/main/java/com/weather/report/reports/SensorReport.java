package com.weather.report.reports;

import java.util.List;
import java.util.SortedMap;

import com.weather.report.model.entities.Measurement;

/**
 * Report describing statistics for a single sensor, extending the common fields
 * of {@link Report}.
 * Statistical values follow the definitions in the README (mean, sample
 * variance, standard deviation). Outliers are measurements whose value differs
 * from the mean by at least two times the standard deviation within the
 * requested interval.
 */
public interface SensorReport extends Report<Double> {

  /**
   * Mean of the sensor measurements in the requested interval (0 when fewer than
   * two
   * measurements are available).
   *
   * @return mean value
   */
  public double getMean();

  /**
   * Sample variance of the measurements in the requested interval (0 when fewer
   * than
   * two measurements are available).
   *
   * @return variance value
   */
  public double getVariance();

  /**
   * Standard deviation of the measurements in the requested interval (0 when
   * fewer than
   * two measurements are available).
   *
   * @return standard deviation
   */
  public double getStdDev();

  /**
   * Minimum measured value in the requested interval.
   *
   * @return minimum measurement value
   */
  public double getMinimumMeasuredValue();

  /**
   * Maximum measured value in the requested interval.
   *
   * @return maximum measurement value
   */
  public double getMaximumMeasuredValue();

  /**
   * Measurements considered outliers in the requested interval.
   *
   * @return list of outlier measurements
   */
  public List<Measurement> getOutliers();

  /**
   * Returns a value-based histogram of the non-outlier measurements associated
   * with this sensor in the interval considered by the report.
   *
   * The histogram is represented as a sorted map where:
   * - each key is a {@link Report.Range}{@code <Double>} describing a numeric
   * value interval with a start and an end bound;
   * - each value is the number of non-outlier measurements whose value falls
   * into that interval according to the histogram convention described below.
   *
   * The histogram is built as follows:
   * - all measurements classified as outliers for this report are excluded;
   * - among the remaining measurements, the minimum and maximum values define
   * the effective value span;
   * - this span is partitioned into a fixed number of buckets (exactly 20),
   * each represented by a {@code Range<Double>} with a constant width;
   * - all buckets except the last one are left-closed and right-open [start,
   * end);
   * the last bucket is [start, end] so that the maximum observed value is
   * included.
   *
   * The bucketing strategy (20 equal-width buckets over the [min, max] span of
   * non-outlier measurements) must be applied consistently to all sensors;
   * the actual numeric bounds of each {@code Range<Double>} depend on the
   * distribution of the values observed for the specific sensor and interval.
   *
   * The returned {@code SortedMap} is ordered by ascending bucket start value.
   * If no non-outlier measurements exist for the interval considered by the
   * report, the returned map may be empty.
   *
   * @return a sorted map associating each {@code Range<Double>} bucket with the
   *         count of non-outlier measurements in that value range
   */
  @Override
  public SortedMap<Range<Double>, Long> getHistogram();

}
