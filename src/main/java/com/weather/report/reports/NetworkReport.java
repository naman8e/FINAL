package com.weather.report.reports;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

/**
 * Report with aggregate metrics at network level, built on top of the common
 * fields
 * defined in {@link Report}.
 * All values refer to the requested time interval (when provided).
 */
public interface NetworkReport extends Report<LocalDateTime> {

  /**
   * Gateways with the highest number of measurements for the network.
   *
   * @return collection of gateway codes (ties are all included, order
   *         unspecified)
   */
  public Collection<String> getMostActiveGateways();

  /**
   * Gateways with the lowest number of measurements for the network.
   *
   * @return collection of gateway codes (ties are all included, order
   *         unspecified)
   */
  public Collection<String> getLeastActiveGateways();

  /**
   * Ratio between measurements of each gateway and the total measurements of the
   * network, expressed as a percentage.
   *
   * @return map {@code <gatewayCode, ratio>}
   */
  public Map<String, Double> getGatewaysLoadRatio();

  /**
   * Returns the number of measurements included in this network report,
   * grouped into consecutive time buckets.
   *
   * The method only considers measurements that belong to the current Network
   * and that fall within an effective interval [effectiveStart, effectiveEnd],
   * defined as follows:
   *
   * - if startDate is non-null, effectiveStart is the parsed value of startDate;
   * otherwise, effectiveStart is the timestamp of the earliest measurement
   * available for this Network (or null if no measurements exist);
   * - if endDate is non-null, effectiveEnd is the parsed value of endDate;
   * otherwise, effectiveEnd is the timestamp of the latest measurement
   * available for this Network (or null if no measurements exist).
   *
   * If either effectiveStart or effectiveEnd is null (i.e. there are no
   * measurements for this Network), the method returns an empty map.
   *
   * Once [effectiveStart, effectiveEnd] has been determined, the bucket
   * granularity is selected as follows:
   *
   * - if both effectiveStart and effectiveEnd are non-null and the duration
   * between them is less than or equal to 48 hours, the interval is
   * partitioned into hourly buckets;
   * - in all other cases, the interval is partitioned into daily buckets.
   *
   * Buckets always cover sub-intervals of [effectiveStart, effectiveEnd] and
   * are built by intersecting the logical hour/day units with the effective
   * interval:
   *
   * - for hourly buckets, each bucket normally covers a full hour
   * (yyyy-MM-dd HH:00:00 -> yyyy-MM-dd HH:59:59), except for the first and the
   * last bucket, whose bounds are truncated to effectiveStart and effectiveEnd
   * respectively;
   *
   * - for daily buckets, each bucket normally covers a full calendar day
   * (yyyy-MM-dd 00:00:00 -> yyyy-MM-dd 23:59:59), except for the first and the
   * last bucket, whose bounds are truncated to effectiveStart and effectiveEnd
   * respectively.
   *
   * Each entry of the returned map represents a single time bucket:
   *
   * - the key is a {@code Range<LocalDateTime>} instance that stores the exact
   * start and end instants of the bucket together with its logical unit
   * (HOUR or DAY);
   *
   * - the value is the total number of measurements whose timestamp falls
   * into that bucket according to the histogram convention:
   * all buckets except the last one are left-closed and right-open
   * [start, end), while the last bucket is [start, end] so that the
   * maximum timestamp is included.
   *
   * Buckets are contiguous and fully cover the [effectiveStart, effectiveEnd]
   * interval. The first and the last bucket may represent only partial hours
   * or days, depending on the actual effectiveStart / effectiveEnd values.
   *
   * The returned {@code SortedMap} is sorted by ascending bucket start time.
   *
   * @return a sorted map where each key is a {@code Range<LocalDateTime>}
   *         describing a time bucket and each value is the number of measurements
   *         in that bucket
   */
  @Override
  public SortedMap<Range<LocalDateTime>, Long> getHistogram();

}
