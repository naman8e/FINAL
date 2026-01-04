package com.weather.report.operations;

import com.weather.report.exceptions.*;
import com.weather.report.model.entities.*;
import com.weather.report.model.UserType;
import com.weather.report.reports.Report.Range;
import com.weather.report.repositories.CRUDRepository;
import com.weather.report.reports.*;
import com.weather.report.services.AlertingService;
import com.weather.report.WeatherReport;
import com.weather.report.persistence.PersistenceManager;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GatewayOperationsImpl implements GatewayOperations {

    private final CRUDRepository<Gateway, String> gatewayRepo = new CRUDRepository<>(Gateway.class);
    private final CRUDRepository<User, String> userRepo = new CRUDRepository<>(User.class);
    private static final Pattern GW_CODE_PATTERN = Pattern.compile("^GW_\\d{4}$");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(WeatherReport.DATE_FORMAT);

    @Override
    public Gateway createGateway(String code, String name, String description, String username)
            throws IdAlreadyInUseException, InvalidInputDataException, UnauthorizedException {
        if (code == null || username == null) throw new InvalidInputDataException("Mandatory data missing");
        checkMaintainer(username);
        if (!GW_CODE_PATTERN.matcher(code).matches()) throw new InvalidInputDataException("Invalid format");
        if (gatewayRepo.read(code) != null) throw new IdAlreadyInUseException("Exists");

        Gateway gateway = new Gateway(code, name, description);
        gateway.setCreatedBy(username);
        gateway.setCreatedAt(LocalDateTime.now());
        return gatewayRepo.create(gateway);
    }

    @Override
    public Gateway updateGateway(String code, String name, String description, String username)
            throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
        if (code == null || username == null) throw new InvalidInputDataException("Missing data");
        checkMaintainer(username);
        Gateway existingGateway = gatewayRepo.read(code);
        if (existingGateway == null) throw new ElementNotFoundException("Not found");

        if (name != null) existingGateway.setName(name);
        if (description != null) existingGateway.setDescription(description);
        existingGateway.setModifiedBy(username);
        existingGateway.setModifiedAt(LocalDateTime.now());
        return gatewayRepo.update(existingGateway);
    }

    @Override
    public Gateway deleteGateway(String code, String username)
            throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
        if (code == null || username == null) throw new InvalidInputDataException("Missing data");
        checkMaintainer(username);
        Gateway gateway = gatewayRepo.delete(code);
        if (gateway == null) throw new ElementNotFoundException("Not found");
        AlertingService.notifyDeletion(username, code, Gateway.class);
        return gateway;
    }

    @Override
    public Collection<Gateway> getGateways(String... codes) {
        if (codes == null || codes.length == 0) return gatewayRepo.read();
        return Arrays.stream(codes).map(gatewayRepo::read).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public Parameter createParameter(String gatewayCode, String code, String name, String description, double value, String username)
            throws IdAlreadyInUseException, InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
        if (gatewayCode == null || code == null || username == null) throw new InvalidInputDataException("Missing");
        checkMaintainer(username);
        Gateway gateway = gatewayRepo.read(gatewayCode);
        if (gateway == null) throw new ElementNotFoundException("Gateway missing");
        if (gateway.getParameters().stream().anyMatch(p -> p.getCode().equals(code))) throw new IdAlreadyInUseException("Exists");

        Parameter parameter = new Parameter();
        parameter.setCode(code); 
        parameter.setName(name); 
        parameter.setDescription(description); 
        parameter.setValue(value); 
        parameter.setGateway(gateway);
        gateway.getParameters().add(parameter);
        gatewayRepo.update(gateway);
        return parameter;
    }

    @Override
    public Parameter updateParameter(String gatewayCode, String code, double value, String username)
            throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
        if (gatewayCode == null || code == null || username == null) throw new InvalidInputDataException("Missing");
        checkMaintainer(username);
        Gateway gateway = gatewayRepo.read(gatewayCode);
        if (gateway == null) throw new ElementNotFoundException("Gateway missing");
        Parameter parameter = gateway.getParameters().stream().filter(p -> p.getCode().equals(code)).findFirst()
                .orElseThrow(() -> new ElementNotFoundException("Param missing"));
        parameter.setValue(value);
        gatewayRepo.update(gateway);
        return parameter;
    }

    @Override
    public GatewayReport getGatewayReport(String code, String start, String end)
            throws ElementNotFoundException, InvalidInputDataException {
        if (code == null) throw new InvalidInputDataException("Code null");
        Gateway gateway = gatewayRepo.read(code);
        if (gateway == null) throw new ElementNotFoundException("Not found");

        GatewayReportImpl report = new GatewayReportImpl(code, start, end);
        report.setBatteryChargePercentage(gateway.getParameters().stream()
                .filter(p -> p.getCode().equals(Parameter.BATTERY_CHARGE_PERCENTAGE_CODE))
                .map(Parameter::getValue).findFirst().orElse(0.0));

        List<Measurement> measurements = getFilteredMeasurements(code, start, end);
        report.setNumberOfMeasurements(measurements.size());
        
        if (measurements.size() >= 1) {
            calculateSensorsStats(measurements, report, gateway);
            if (measurements.size() >= 2) {
                calculateHistogram(measurements, report);
            }
        }

        return report;
    }

    private List<Measurement> getFilteredMeasurements(String code, String start, String end) {
        EntityManager entityManager = PersistenceManager.getEntityManager();
        try {
            String jpql = "SELECT m FROM Measurement m WHERE m.gatewayCode = :code";
            if (start != null) jpql += " AND m.timestamp >= :start";
            if (end != null) jpql += " AND m.timestamp <= :end";
            jpql += " ORDER BY m.timestamp ASC";
            
            var query = entityManager.createQuery(jpql, Measurement.class).setParameter("code", code);
            if (start != null) query.setParameter("start", LocalDateTime.parse(start, FORMATTER));
            if (end != null) query.setParameter("end", LocalDateTime.parse(end, FORMATTER));
            return query.getResultList();
        } finally { entityManager.close(); }
    }

    private void calculateSensorsStats(List<Measurement> measurements, GatewayReportImpl report, Gateway gateway) {
        Map<String, List<Measurement>> bySensor = measurements.stream().collect(Collectors.groupingBy(Measurement::getSensorCode));
        Map<String, Long> counts = bySensor.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (long)e.getValue().size()));
        
        long maxCount = Collections.max(counts.values());
        long minCount = Collections.min(counts.values());
        
        report.setMostActiveSensors(counts.entrySet().stream().filter(e -> e.getValue() == maxCount).map(Map.Entry::getKey).collect(Collectors.toList()));
        report.setLeastActiveSensors(counts.entrySet().stream().filter(e -> e.getValue() == minCount).map(Map.Entry::getKey).collect(Collectors.toList()));
        
        double total = measurements.size();
        report.setSensorsLoadRatio(counts.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (e.getValue()/total)*100.0)));

        double expectedMean = gateway.getParameters().stream().filter(p -> p.getCode().equals(Parameter.EXPECTED_MEAN_CODE)).map(Parameter::getValue).findFirst().orElse(0.0);
        double expectedStdDev = gateway.getParameters().stream().filter(p -> p.getCode().equals(Parameter.EXPECTED_STD_DEV_CODE)).map(Parameter::getValue).findFirst().orElse(0.0);
        
        List<String> outlierSensors = new ArrayList<>();
        if (measurements.size() >= 2) {
            bySensor.forEach((sensorCode, sensorMeasurements) -> {
                double meanValue = sensorMeasurements.stream().mapToDouble(Measurement::getValue).average().orElse(0.0);
                if (Math.abs(meanValue - expectedMean) >= 2 * expectedStdDev) outlierSensors.add(sensorCode);
            });
        }
        report.setOutlierSensors(outlierSensors);
    }

    private void calculateHistogram(List<Measurement> measurements, GatewayReportImpl report) {
        List<Duration> durations = new ArrayList<>();
        for (int i = 0; i < measurements.size() - 1; i++) {
            durations.add(Duration.between(measurements.get(i).getTimestamp(), measurements.get(i+1).getTimestamp()));
        }
        
        Duration minDuration = Collections.min(durations);
        Duration maxDuration = Collections.max(durations);
        long totalNanos = maxDuration.toNanos() - minDuration.toNanos();
        double stepNanos = totalNanos / 20.0;

        SortedMap<Range<Duration>, Long> histogram = new TreeMap<>(Comparator.comparing(Range::getStart));
        
        for (int i = 0; i < 20; i++) {
            final Duration start = minDuration.plusNanos((long) (i * stepNanos));
            final Duration end = (i == 19) ? maxDuration : minDuration.plusNanos((long) ((i + 1) * stepNanos));
            final boolean isLastBucket = (i == 19);

            Range<Duration> range = new Range<Duration>() {
                @Override public Duration getStart() { return start; }
                @Override public Duration getEnd() { return end; }

                @Override
                public boolean contains(Duration durationValue) {
                    if (durationValue == null) return false;
                    boolean lowerBound = durationValue.compareTo(start) >= 0;
                    boolean upperBound = isLastBucket ? durationValue.compareTo(end) <= 0 : durationValue.compareTo(end) < 0;
                    return lowerBound && upperBound;
                }
            };

            long count = durations.stream().filter(range::contains).count();
            histogram.put(range, count);
        }
        report.setHistogram(histogram);
    }

    private void checkMaintainer(String username) throws UnauthorizedException {
        User user = userRepo.read(username);
        if (user == null || user.getType() != UserType.MAINTAINER) throw new UnauthorizedException("Unauthorized");
    }
}