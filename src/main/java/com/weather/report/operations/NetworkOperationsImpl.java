package com.weather.report.operations;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.HashMap;

import com.weather.report.WeatherReport;
import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.IdAlreadyInUseException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.entities.Measurement;
import com.weather.report.model.entities.Network;
import com.weather.report.model.entities.Operator;
import com.weather.report.model.entities.User;
import com.weather.report.reports.NetworkReport;
import com.weather.report.reports.NetworkReportImpl;
import com.weather.report.repositories.CRUDRepository;
import com.weather.report.repositories.MeasurementRepository;
import com.weather.report.utils.ValidationUtils;
import com.weather.report.services.AlertingService;

//implementation of networkoperations interface (required by readme.md)

public class NetworkOperationsImpl implements NetworkOperations {

    private final CRUDRepository<Network, String> networkRepo;
    private final CRUDRepository<Operator, String> operatorRepo;
    private final MeasurementRepository measurementRepo;

    public NetworkOperationsImpl() {
        this.networkRepo = new CRUDRepository<>(Network.class);
        this.operatorRepo = new CRUDRepository<>(Operator.class);
        this.measurementRepo = new MeasurementRepository();
    }

    @Override
    public Network createNetwork(String code, String name, String description, String username)
            throws IdAlreadyInUseException, InvalidInputDataException, UnauthorizedException {
        ValidationUtils.validateNetworkCode(code);
        User user = ValidationUtils.validateMaintainerUser(username);

        Network existing = networkRepo.read(code);
        if (existing != null) {
            throw new IdAlreadyInUseException("Network with code '" + code + "' already exsists ");
        }

        Network network = new Network(code, name, description);

        network.setCreatedBy(user.getUsername());

        network.setCreatedAt(LocalDateTime.now());

        network = networkRepo.create(network);
        return network;
    }

    @Override
    public Network updateNetwork(String code, String name, String description, String username)
            throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
        ValidationUtils.validateNotNullOrEmpty(code, "Network code");

        User user = ValidationUtils.validateMaintainerUser(username);

        Network network = networkRepo.read(code);
        if (network == null) {
            throw new ElementNotFoundException("Network with code '" + code + "' not found");
        }

        network.setName(name);
        network.setDescription(description);
        network.setModifiedBy(user.getUsername());
        network.setModifiedAt(LocalDateTime.now());

        network = networkRepo.update(network);

        return network;
    }

    @Override
    public Network deleteNetwork(String code, String username)
            throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
        ValidationUtils.validateNotNullOrEmpty(code, "Network code");

        User user = ValidationUtils.validateMaintainerUser(username);

        Network network = networkRepo.read(code);
        if (network == null) {
            throw new ElementNotFoundException("Network with code '" + code + "' not found");
        }
        networkRepo.delete(code);

        AlertingService.notifyDeletion(user.getUsername(), code, Network.class);

        return network;
    }

    @Override
    public Collection<Network> getNetworks(String... codes) {
        if (codes == null || codes.length == 0) {
            return networkRepo.read();
        }

        List<Network> allNetworks = networkRepo.read();
        return allNetworks.stream()
                .filter(n -> {
                    for (String code : codes) {
                        if (n.getCode().equals(code)) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Operator createOperator(String firstName, String lastName, String email, String phoneNumber, String username)
            throws InvalidInputDataException, IdAlreadyInUseException, UnauthorizedException {
        ValidationUtils.validateNotNullOrEmpty(firstName, "First name");
        ValidationUtils.validateNotNullOrEmpty(lastName, "Last name");
        ValidationUtils.validateNotNullOrEmpty(email, "Email");
        // ph no. can be null

        ValidationUtils.validateMaintainerUser(username);

        Operator existing = operatorRepo.read(email);
        if (existing != null) {
            throw new IdAlreadyInUseException("Operator with email '" + email + "' already exists");

        }
        Operator operator = new Operator(firstName, lastName, email, phoneNumber);

        operator = operatorRepo.create(operator);
        return operator;
    }

    @Override
    public Network addOperatorToNetwork(String networkCode, String operatorEmail, String username)
            throws ElementNotFoundException, InvalidInputDataException, UnauthorizedException {
        ValidationUtils.validateNotNullOrEmpty(networkCode, "Network code");
        ValidationUtils.validateNotNullOrEmpty(operatorEmail, "Operator email");

        ValidationUtils.validateMaintainerUser(username);

        Network network = networkRepo.read(networkCode);
        if (network == null) {
            throw new ElementNotFoundException("Network with code '" + networkCode + "' not found");
        }
        Operator operator = operatorRepo.read(operatorEmail);
        if (operator == null) {
            throw new ElementNotFoundException("Operator with email '" + operatorEmail + "' not found");
        }
        network.addOperator(operator);
        network = networkRepo.update(network);
        return network;
    }

    @Override
    public NetworkReport getNetworkReport(String code, String startDate, String endDate)
            throws InvalidInputDataException, ElementNotFoundException {
        ValidationUtils.validateNotNullOrEmpty(code, "Network code");

        Network network = networkRepo.read(code);
        if (network == null) {
            throw new ElementNotFoundException("Network with code '" + code + "' not found");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(WeatherReport.DATE_FORMAT);
        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate, formatter) : null;
        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate, formatter) : null;

        List<Measurement> allMeasurements = measurementRepo.read();

        List<Measurement> measurements = allMeasurements.stream()
                .filter(m -> code.equals(m.getNetworkCode()))
                .filter(m -> isInTimeRange(m.getTimestamp(), start, end))
                .collect(Collectors.toList());

        if (measurements.isEmpty()) {
            return new NetworkReportImpl(
                    code,
                    startDate,
                    endDate,
                    0,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new HashMap<>(),
                    new TreeMap<>());
        }

        Map<String, Long> gatewayCountMap = measurements.stream()
                .collect(Collectors.groupingBy(
                        Measurement::getGatewayCode,
                        Collectors.counting()));

        long maxCount = gatewayCountMap.values().stream()
                .max(Long::compareTo)
                .orElse(0L);

        Collection<String> mostActive = gatewayCountMap.entrySet().stream()
                .filter(e -> e.getValue() == maxCount)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        long minCount = gatewayCountMap.values().stream()
                .min(Long::compareTo)
                .orElse(0L);
        Collection<String> leastActive = gatewayCountMap.entrySet().stream()
                .filter(e -> e.getValue() == minCount)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        long totalMeasurements = measurements.size();
        Map<String, Double> loadRatios = new HashMap<>();

        for (Map.Entry<String, Long> entry : gatewayCountMap.entrySet()) {
            double percentage = (entry.getValue() * 100.0) / totalMeasurements;
            loadRatios.put(entry.getKey(), percentage);
        }

        SortedMap<NetworkReport.Range<LocalDateTime>, Long> histogram = buildTimeHistogram(measurements, start, end);

        return new NetworkReportImpl(
                code,
                startDate,
                endDate,
                totalMeasurements,
                mostActive,
                leastActive,
                loadRatios,
                histogram);
    }

    private boolean isInTimeRange(LocalDateTime timestamp, LocalDateTime start, LocalDateTime end) {
        // inclusive lowerbound
        if (start != null && timestamp.isBefore(start)) {
            return false;
        }
        // inclusive upper bound
        if (end != null && timestamp.isAfter(end)) {
            return false;
        }
        return true;
    }

    private SortedMap<NetworkReport.Range<LocalDateTime>, Long> buildTimeHistogram(
            List<Measurement> measurements,
            LocalDateTime requestedStart,
            LocalDateTime requestedEnd) {
        SortedMap<NetworkReport.Range<LocalDateTime>, Long> histogram = new TreeMap<>(
                Comparator.comparing(NetworkReport.Range::getStart));
        if (measurements.isEmpty()) {
            return histogram;
        }
        // effective time range -_-
        LocalDateTime effectiveStart = requestedStart;
        LocalDateTime effectiveEnd = requestedEnd;

        if (effectiveStart == null) {
            effectiveStart = measurements.stream()
                    .map(Measurement::getTimestamp)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
        }
        if (effectiveEnd == null) {
            effectiveEnd = measurements.stream()
                    .map(Measurement::getTimestamp)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
        }

        long hoursBetween = ChronoUnit.HOURS.between(effectiveStart, effectiveEnd);
        boolean useHourly = hoursBetween <= 48;
        ChronoUnit unit = useHourly ? ChronoUnit.HOURS : ChronoUnit.DAYS;

        List<NetworkReportImpl.TimeRange> buckets = createTimeBuckets(
                effectiveStart, effectiveEnd, unit);

        for (NetworkReportImpl.TimeRange bucket : buckets) {
            long count = measurements.stream()
                    .filter(m -> bucket.contains(m.getTimestamp()))
                    .count();

            histogram.put(bucket, count);
        }

        return histogram;
    }

    private List<NetworkReportImpl.TimeRange> createTimeBuckets(
            LocalDateTime start, LocalDateTime end, ChronoUnit unit) {

        List<NetworkReportImpl.TimeRange> buckets = new ArrayList<>();
        LocalDateTime current = start;

        while (current.isBefore(end) || current.equals(end)) {
            LocalDateTime bucketStart = current;
            LocalDateTime bucketEnd;

            if (unit == ChronoUnit.HOURS) {
                // Hourly: truncate to hour boundaries
                bucketStart = current.truncatedTo(ChronoUnit.HOURS);
                bucketEnd = bucketStart.plusHours(1).minusNanos(1);
            } else {
                // Daily: truncate to day boundaries
                bucketStart = current.truncatedTo(ChronoUnit.DAYS);
                bucketEnd = bucketStart.plusDays(1).minusNanos(1);
            }

            // Truncate first bucket to actual start
            if (buckets.isEmpty()) {
                bucketStart = start;
            }

            // Truncate last bucket to actual end
            if (bucketEnd.isAfter(end)) {
                bucketEnd = end;
            }

            boolean isLast = bucketEnd.equals(end) || bucketEnd.isAfter(end);

            buckets.add(new NetworkReportImpl.TimeRange(bucketStart, bucketEnd, unit, isLast));

            if (isLast) {
                break;
            }

            current = unit == ChronoUnit.HOURS ? bucketStart.plusHours(1) : bucketStart.plusDays(1);
        }

        return buckets;
    }

}
