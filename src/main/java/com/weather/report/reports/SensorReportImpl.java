package com.weather.report.reports;

import java.util.List;
import java.util.SortedMap;

import com.weather.report.model.entities.Measurement;

public class SensorReportImpl implements SensorReport {

    private final String code;
    private final String startDate;
    private final String endDate;
    private final long n;

    private final double mean;
    private final double variance;
    private final double stdDev;

    private final double minMeasured;
    private final double maxMeasured;

    private final List<Measurement> outliers;
    private final SortedMap<Report.Range<Double>, Long> histogram;

    public SensorReportImpl(
        String code,
        String startDate,
        String endDate,
        long n,
        double mean,
        double variance,
        double stdDev,
        double minMeasured,
        double maxMeasured,
        List<Measurement> outliers,
        SortedMap<Report.Range<Double>, Long> histogram) {

      this.code = code;
      this.startDate = startDate;
      this.endDate = endDate;
      this.n = n;
      this.mean = mean;
      this.variance = variance;
      this.stdDev = stdDev;
      this.minMeasured = minMeasured;
      this.maxMeasured = maxMeasured;
      this.outliers = outliers;
      this.histogram = histogram;
    }

    @Override
    public String getCode() {
      return code;
    }

    @Override
    public String getStartDate() {
      return startDate;
    }

    @Override
    public String getEndDate() {
      return endDate;
    }

    @Override
    public long getNumberOfMeasurements() {
      return n;
    }

    @Override
    public double getMean() {
      return mean;
    }

    @Override
    public double getVariance() {
      return variance;
    }

    @Override
    public double getStdDev() {
      return stdDev;
    }

    @Override
    public double getMinimumMeasuredValue() {
      return minMeasured;
    }

    @Override
    public double getMaximumMeasuredValue() {
      return maxMeasured;
    }

    @Override
    public List<Measurement> getOutliers() {
      return outliers;
    }

    @Override
    public SortedMap<Report.Range<Double>, Long> getHistogram() {
      return histogram;
    }
}