package com.weather.report.reports;

import java.util.SortedMap;

/**
 * Common metadata for every report type returned by the Weather Report system.
 * The code identifies the element (network, gateway or sensor). Dates are the
 * same strings received as input (may be {@code null} to represent an open
 * bound).
 * The number of measurements counts the values considered in the requested
 * interval.
 * 
 * @param <HistogramRangeUnit> type used as the bound of the histogram ranges
 *                             (for example {@code LocalDateTime},
 *                             {@code Duration} or {@code Double})
 */
public interface Report<HistogramRangeUnit> {

  /**
   * Unique code of the reported element.
   *
   * @return element code
   */
  public String getCode();

  /**
   * Lower bound of the interval (inclusive), or {@code null} when no bound was
   * provided.
   *
   * @return start date string in {@code WeatherReport.DATE_FORMAT}, or
   *         {@code null}
   */
  public String getStartDate();

  /**
   * Upper bound of the interval (inclusive), or {@code null} when no bound was
   * provided.
   *
   * @return end date string in {@code WeatherReport.DATE_FORMAT}, or {@code null}
   */
  public String getEndDate();

  /**
   * Total measurements considered for the report within the requested interval.
   *
   * @return number of measurements
   */
  public long getNumberOfMeasurements();

  public SortedMap<Range<HistogramRangeUnit>, Long> getHistogram();

  /**
   * Generic one-dimensional range used as a key in histogram-based reports.
   * Unless otherwise stated, all {@code Range} instances produced by the
   * Weather Report reporting APIs follow a left-closed, right-open convention:
   * a value {@code v} belongs to a bucket if {@code start <= v < end}.
   * The only exception is the last bucket of each histogram, which is
   * treated as {@code [start, end]} so that the global maximum value
   * observed in the interval is always included in some bucket.
   *
   * @param <T> type of the range bounds (for example {@code LocalDateTime},
   *            {@code Duration} or {@code Double})
   */
  public interface Range<T> {

    /**
     * Returns the start value of this range.
     * For all histograms in the Weather Report system ranges are
     * left-closed with respect to the start: a value {@code v}
     * can belong to a bucket only if {@code start <= v}.
     */
    T getStart();

    /**
     * Returns the end value of this range.
     * For all histograms in the Weather Report system ranges are
     * right-open, except for the last bucket of each histogram:
     * a value {@code v} belongs to a bucket if
     * {@code v < end}, or {@code v <= end} when the bucket is
     * the last one for that histogram.
     */
    T getEnd();

    /**
     * Checks whether the given value falls inside this range,
     * according to the histogram convention adopted by the report
     * that created it (left-closed, right-open for all buckets
     * except the last one, which may also include its upper bound).
     *
     * @param value value to check
     * @return {@code true} if the value falls inside this range
     */
    boolean contains(T value);
  }

}
