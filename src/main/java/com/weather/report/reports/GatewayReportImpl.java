package com.weather.report.reports;

import java.time.Duration;
import java.util.*;

public class GatewayReportImpl implements GatewayReport {
    private String code;
    private String startDate;
    private String endDate;
    private long numberOfMeasurements;
    private Collection<String> mostActiveSensors = new ArrayList<>();
    private Collection<String> leastActiveSensors = new ArrayList<>();
    private Map<String, Double> sensorsLoadRatio = new HashMap<>();
    private Collection<String> outlierSensors = new ArrayList<>();
    private double batteryChargePercentage;
    private SortedMap<Range<Duration>, Long> histogram = new TreeMap<>(Comparator.comparing(Range::getStart));

    public GatewayReportImpl(String code, String startDate, String endDate) {
        this.code = code;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override public String getCode() { return code; }
    @Override public String getStartDate() { return startDate; }
    @Override public String getEndDate() { return endDate; }
    @Override public long getNumberOfMeasurements() { return numberOfMeasurements; }
    @Override public Collection<String> getMostActiveSensors() { return mostActiveSensors; }
    @Override public Collection<String> getLeastActiveSensors() { return leastActiveSensors; }
    @Override public Map<String, Double> getSensorsLoadRatio() { return sensorsLoadRatio; }
    @Override public Collection<String> getOutlierSensors() { return outlierSensors; }
    @Override public double getBatteryChargePercentage() { return batteryChargePercentage; }
    @Override public SortedMap<Range<Duration>, Long> getHistogram() { return histogram; }

    public void setNumberOfMeasurements(long numberOfMeasurements) { this.numberOfMeasurements = numberOfMeasurements; }
    public void setMostActiveSensors(Collection<String> sensors) { this.mostActiveSensors = sensors; }
    public void setLeastActiveSensors(Collection<String> sensors) { this.leastActiveSensors = sensors; }
    public void setSensorsLoadRatio(Map<String, Double> ratio) { this.sensorsLoadRatio = ratio; }
    public void setOutlierSensors(Collection<String> outliers) { this.outlierSensors = outliers; }
    public void setBatteryChargePercentage(double percentage) { this.batteryChargePercentage = percentage; }
    public void setHistogram(SortedMap<Range<Duration>, Long> histogram) { this.histogram = histogram; }
}