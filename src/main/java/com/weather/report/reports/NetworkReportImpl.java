package com.weather.report.reports;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

//implementation of network report interface

public class NetworkReportImpl implements NetworkReport {

    private final String code;
    private final String startDate;
    private final String endDate;
    private final long numberOfMeasurements;
    private final Collection<String> mostActiveGateways;
    private final Collection<String> leastActiveGateways;
    private final Map<String, Double> gatewaysLoadRatio;
    private final SortedMap<Range<LocalDateTime>, Long> histogram;

    public NetworkReportImpl(
            String code,
            String startDate,
            String endDate,
            long numberOfMeasurements,
            Collection<String> mostActiveGateways,
            Collection<String> leastActiveGateways,
            Map<String, Double> gatewaysLoadRatio,
            SortedMap<Range<LocalDateTime>, Long> histogram) {

        this.code = code;
        this.startDate = startDate;
        this.endDate = endDate;
        this.numberOfMeasurements = numberOfMeasurements;
        this.mostActiveGateways = mostActiveGateways;
        this.leastActiveGateways = leastActiveGateways;
        this.gatewaysLoadRatio = gatewaysLoadRatio;
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

        return numberOfMeasurements;
    }

    @Override
    public Collection<String> getMostActiveGateways() {

        return mostActiveGateways;
    }

    @Override
    public Collection<String> getLeastActiveGateways() {

        return leastActiveGateways;
    }

    @Override
    public Map<String, Double> getGatewaysLoadRatio() {

        return gatewaysLoadRatio;
    }

    @Override
    public SortedMap<Range<LocalDateTime>, Long> getHistogram() {

        return histogram;
    }

    public static class TimeRange implements Range<LocalDateTime> {
        private final LocalDateTime start;
        private final LocalDateTime end;
        private final ChronoUnit unit;
        private final boolean isLast;

        public TimeRange(LocalDateTime start, LocalDateTime end, ChronoUnit unit, boolean isLast) {
            this.start = start;
            this.end = end;
            this.unit = unit;
            this.isLast = isLast;
        }

        @Override
        public LocalDateTime getStart() {
            return start;
        }

        @Override
        public LocalDateTime getEnd() {
            return end;
        }

        public ChronoUnit getUnit() {
            return unit;
        }

        @Override
        public boolean contains(LocalDateTime value) {
            if (value == null) {
                return false;
            }
            boolean afterStart = !value.isBefore(start);

            if (isLast) {
                boolean beforeOrAtEnd = !value.isAfter(end);
                return afterStart && beforeOrAtEnd;

            } else {
                boolean beforeEnd = value.isBefore(end);
                return afterStart && beforeEnd;
            }
        }

        @Override
        public String toString() {
            return String.format("[%s, %s%s %s", start, end, isLast ? "]" : ")", unit);
        }

    }

}
