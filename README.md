Contents:

- 1. [Weather Report](#1-weather-report)
- 2. [Technical description](#2-technical-description)
  - 2.1 [Object model](#21-object-model)
  - 2.2 [Façade](#22-façade)
  - 2.3 [Operations](#23-operations)
  - 2.4 [Repository](#24-repository)
  - 2.5 [Services](#25-services)
  - 2.6 [Exceptions](#26-exceptions)
- 3. [Implementation](#3-implementation)
  - 3.1 [GitFlow and code reviews](#31-gitflow-and-code-reviews)
  - 3.2 [Branch rules](#32-branch-rules)
  - 3.3 [Reporting](#33-reporting)
  - 3.4 [Custom tests](#34-custom-tests)
- 4. [Requirement specifications](#4-requirement-specifications)
  - [R1 – Network](#r1-network)
  - [R2 – Gateway](#r2-gateway)
  - [R3 – Sensor](#r3-sensor)
  - [R4 – Topology](#r4-topology)

---

# 1. Weather Report

The **Weather Report** system manages environmental monitoring data collected from an infrastructure composed of **networks**, **gateways** and **sensors**.  
The goal is to store measurements, configure control rules, produce reports on the collected data, and notify possible anomalous measurements.

## General system functionalities

The Weather Report system provides functionalities for managing monitoring data and the logical structure of the elements.  
The main capabilities of the system include:

- Creation, modification and deletion of networks, gateways, sensors and their associated elements (operators, parameters and thresholds).  
  The system records _who_ created or modified a network, a gateway or a sensor, and _when_ the operation was performed.  
  Deletions of Network, Gateway and Sensor must be notified through the mechanism provided by the system.  
  These operations concern the existence of objects in the system, independently of any hierarchical relationships.
- Association and disassociation between elements (for example linking a gateway to a network, or a sensor to a gateway).  
  Association operations belong to topology management and are distinct from the operations that deal with object creation.
- Import and storage of measurements produced by sensors.
- Analysis of measurements to detect anomalous values based on configured parameters and thresholds.
- Generation of reports at network, gateway and sensor level.
- Notification of threshold violations to the operators associated with the involved networks.

# 2. Technical description

The Weather Report system persists all the data of the model described in the previous section on a relational database, using **Hibernate** as pure JPA provider and **H2** as DBMS.

## 2.1 Object model

### Class `Network`

A _monitoring network_ that represents a logical set of system elements.  
It may have a name and a description, and is uniquely identified by a code.  
A network may have a list of _operators_ responsible for receiving notifications.

#### Network code format

The code of a network must be a string that starts with **"NET\_"** and is followed by **two decimal digits**.

### Class `Operator`

An _operator_ is an entity that receives notifications when a threshold violation is detected.  
It is uniquely identified by its **email address**.  
It has first name, last name and may also have a phone number.  
The same operator may be responsible for multiple networks.

---

### Class `Gateway`

A _gateway_ groups multiple devices that monitor the same physical quantity.  
It can be configured through parameters that provide information about its state or values needed for interpreting the measurements.  
It may have a name and a description, and is uniquely identified by a code.

#### Gateway code format

The code of a gateway must be a string that starts with **"GW\_"** and is followed by **four decimal digits**.

### Class `Parameter`

A _parameter_ is a value associated with the gateway it belongs to.  
It allows storing state or configuration information.  
Each parameter has a unique code **within the gateway**, and may have a name, a description and a numeric value.

#### Special parameters

There are three reserved codes:

- `EXPECTED_MEAN`
- `EXPECTED_STD_DEV`
- `BATTERY_CHARGE`

When present, these parameters are used by the system to compute specific parts of a gateway’s report.

---

### Class `Sensor`

A _sensor_ measures a physical quantity and periodically sends the corresponding measurements.  
It may have a name and a description and is uniquely identified by a code.  
A sensor may have a _threshold_ defined by the user to detect anomalous behaviours.

#### Sensor code format

The code of a sensor must be a string that starts with **"S\_"** and is followed by **six decimal digits**.

### Class `Threshold`

A _threshold_ defines an acceptable limit for the values measured by a sensor.  
It **always** consists of a numeric value and a comparison type that the system must apply to decide whether a measurement is anomalous.

---

### Class `Timestamped`

The `Timestamped` class, extended by `Network`, `Gateway` and `Sensor`, contains the metadata needed to track:

- who created the instance;
- when it was created;
- who last modified it;
- when it was last modified.

### Class `Measurement`

A _measurement_ is generated by a sensor and **always** contains:

- the code of the network, gateway and sensor that produced it;
- a timestamp;
- a numeric value.

The values are read from CSV files. These files are available in:

```
src/main/resources/csv
```

These files have the following header, which defines the structure of each row:

```
date, networkCode, gatewayCode, sensorCode, value
```

Each row represents a single measurement with the following meaning:

- date: timestamp of the measurement, expressed using WeatherReport.DATE_FORMAT
- networkCode: code of the network associated with the measurement
- gatewayCode: code of the gateway that produced the measurement
- sensorCode: code of the sensor that generated the measurement
- value: numeric value measured by the sensor

All CSV files included in the resources directory follow this exact structure.

### Class `User`

The _users_ of the system are **always** characterised by:

- a unique **username**;
- a **type**, which determines the user’s permissions and can take the values:
  - `VIEWER`
  - `MAINTAINER`

Permissions are defined as follows:

- A **VIEWER** user can only perform _read_ operations (consulting data and reports).
- A **MAINTAINER** user can perform both _read_ and _write_ operations (creation, update and deletion of entities and configurations).

## 2.2 Façade

The `WeatherReport` class implements the façade concept, i.e. the main **entry point** to the system. It is the class used by external code to interact with the exposed functionalities.

The façade:

- provides general utility methods, such as user creation and measurement data import;
- exposes the different sets of operations through dedicated access methods, which return the interfaces:
  - `NetworkOperations`
  - `GatewayOperations`
  - `SensorOperations`
  - `TopologyOperations`

In this way, the caller works with a single instance of `WeatherReport`, delegating the specific logic of each domain area to the operations interfaces.

## 2.3 Operations

The **operations interfaces** represent the groups of functionalities offered by the system on the different types of entities:

- `NetworkOperations` for managing networks and operators;
- `GatewayOperations` for managing gateways and their parameters;
- `SensorOperations` for managing sensors and thresholds;
- `TopologyOperations` for managing the associations between networks, gateways and sensors.

Each functional requirement (R1, R2, R3, R4) is fulfilled by implementing the corresponding `*Operations` interface.  
The concrete implementations contain the application logic and use the repositories to access persisted data.

### Class `OperationsFactory`

`OperationsFactory` is the class responsible for providing concrete instances of the operations interfaces.  
It exposes static methods such as:

- `getNetworkOperations()`
- `getGatewayOperations()`
- `getSensorOperations()`
- `getTopologyOperations()`

which must return the actual implementations to be used in the rest of the system.

The factory is the **central configuration point** for the operations.

## 2.4 Repository

Access to the database is encapsulated in **repository** classes, whose purpose is to expose methods to read and write entities. The advantages of this approach are:

- persistence logic is concentrated in a single layer;
- the rest of the application code does not depend on Hibernate or H2 details;
- it becomes easier to replace or extend the persistence layer.

### Class `CRUDRepository`

`CRUDRepository` is a generic class implementing the basic operations for all entities persisted in the system.  
It exposes the fundamental methods to:

- create new entities;
- read single entities or sets of entities;
- update existing entities;
- delete entities.

The operations use `CRUDRepository` to perform standard database operations, without duplicating data access logic.

### Class `MeasurementRepository`

`MeasurementRepository` is a repository specific to the `Measurement` entity.  
The class extends `CRUDRepository`, specifying the appropriate generic types, in order to provide a central access point to persistence operations on measurements.  
This repository is conceived as the place where any future read operations based on criteria specific to the structure of the `Measurement` entity should be added.  
In this way, selection logic is not scattered across multiple parts of the system, and there is a single extension point for advanced queries.

## 2.5 Services

**Services** are classes exposing reusable methods that are not tied to a single entity but used to perform cross-cutting system functionalities.  
They are invoked by operations or other parts of the code when specific behaviours are needed, such as sending notifications or importing data.

### Notification service

`AlertingService` is the service used to manage notifications.  
The class is provided already implemented and offers two public methods:

- `public static void notifyThresholdViolation(Collection<Operator> operators, String sensorCode)`  
  used to notify the operators of a network when an out-of-threshold value is detected by a sensor;

- `public static void notifyDeletion(String username, String code, Class<?> elementClass)`  
  used to notify the deletion of an element of type `Network`, `Gateway` or `Sensor`.  
  The method receives:
  - the username of the user performing the deletion;
  - the unique code of the deleted element;
  - the class of the element, to distinguish the type of entity involved.

These methods must be invoked at the appropriate points, in particular when threshold violations of measured values are detected or when elements of the hierarchy are deleted.

### Measurement import service

`DataImportingService` is the service responsible for importing measurements from CSV files.

It exposes the public method:

- `public static void storeMeasurements(String filePath)`

which:

- reads the measurements from the CSV files;
- creates the corresponding `Measurement` instances;
- saves them to the database through the repositories.

Inside the class there is the private method:

- `private static void checkMeasurement(Measurement measurement)`

which must be called after saving each measurement.  
This method checks whether the newly inserted value violates an eventual threshold associated with the sensor that generated the measurement and, in case of violation, must invoke the appropriate `AlertingService` method to notify the issue to the relevant operators.

## 2.6 Exceptions

All system-specific exceptions extend the `WeatherReportException` class, which acts as a common superclass.  
The described exceptions have no priority among them: the order in which they may be thrown is not relevant when an operation violates multiple conditions simultaneously.

### Exception `InvalidInputDataException`

It is thrown when invalid, missing or non-conforming data are provided for mandatory attributes.  
Values for optional fields must not trigger this exception.

### Exception `IdAlreadyInUseException`

It is thrown when an attempt is made to create a new element using a unique code that is already present in the system.

### Exception `ElementNotFoundException`

It is thrown when the code of an element that is not contained in the system is provided.

### Exception `UnauthorizedException`

It is thrown when the username passed to the operation:

- does not correspond to any existing user, or
- corresponds to a user who does not have the required permissions to execute the operation.

# 3. Implementation

The implementation of the system is organised into four distinct requirements:

- three individual requirements (R1, R2, R3), independent from each other;
- one integration requirement (R4), which builds on the previous ones.

For each individual requirement (R1, R2, R3), the functionalities to be implemented are organised into two main blocks:

- management of the system elements relevant to the requirement (creation, update, deletion, reading);
- reporting, i.e. computation of aggregate values and statistics on the measurements.

## 3.1 GitFlow and code reviews

During the implementation of the assignment, all team members must follow the GitFlow
workflow as described in this documentation:

- GitFlow specification:  
  https://git-oop.polito.it/labs/docs/-/blob/main/Git/GitFlow_en.md

Each feature must be developed in its own branch following the workflow rules
defined in the document above. All merges into the `main` branch line must
be performed exclusively through Merge Requests.

Code reviews are mandatory: every Merge Request must be evaluated using the
checklist provided in the project repository:

- [Code Review Checklist](./checklist.md)

Team members are required to apply all points of the checklist when reviewing their
teammates’ work before approving a Merge Request.

## 3.2 Branch rules

Requirements R1, R2 and R3 must be developed independently, each by a single team member.  
Each requirement must be implemented on a dedicated branch, whose name must **start with** the following prefix:

- `X-rN`

where:

- `X` is any integer number (usually the number of the issue);
- `N` is the index of the requirement (`1`, `2`, `3` or `4`).

Any additional suffix after this is allowed separated by `-` (for example, `1-r2-network`, `3-r1-feature-x`).  
The test system only considers the `X-rN` prefix to determining which requirement the branch refers to.
This scheme allows the test system to recognise which requirement a given branch refers to and to execute the appropriate test suite.  
In the branches related to individual requirements (R1, R2, R3), only commits from the single team member in charge of that requirement are allowed.

Requirement R4 is an integration requirement: it shall be addressed after all the implementations of the individual requirements have been merged into the `main` branch.  
In this context it is possible to perform refactoring, harmonise design choices, and develop functionalities that directly depend on the results of requirements R1, R2 and R3.  
On the branch dedicated to R4, commits from all team members are allowed.

## 3.3 Reporting

The report returned as output by the functions that compute it is based on the `startDate` and `endDate` parameters. These parameters:

- do not consider the timezone, i.e. they are expressed as absolute dates;
- are optional (they may be _null_);
- are in the `DATE_FORMAT` format defined in `WeatherReport`;
- delimit the inclusive time interval of the measurements to consider;
- if the value is null, the interval is not limited in that direction (for example: `startDate = null` means that there is no lower bound; `endDate = null` means that there is no upper bound).

The reporting sections defined in the various requirements include the computation of a specific histogram (based on the current type of report) and some basic statistical values on the measurements:

- mean;
- sample variance;
- standard deviation;
- identification of outliers.

If the number of available measurements is less than 2, the values of variance and standard deviation are not meaningful and must be set to `0`, and the possible set of outliers is empty.

### Mean

Given a set of \(n\) measurements, the mean is defined as:

$$
\overline{x} = \frac{1}{n} \sum_{i=1}^{n} x_i
$$

### Variance

Sample variance is used, suitable for finite sets of measurements:

$$
\sigma^2 = \frac{1}{n - 1} \sum_{i=1}^{n} (x_i - \overline{x})^2
$$

### Standard deviation

Standard deviation is defined as:

$$
\sigma = \sqrt{\sigma^2}
$$

### Outlier

A measurement $$ x_i $$ is considered an outlier if it differs from the mean by at least two times the standard deviation:

$$
\left| x_i - \overline{x} \right| \ge 2 \sigma
$$

### Histogram range semantics

All histograms in the Weather Report system are represented using
`Report.Range<T>` keys. Unless otherwise stated:

- ranges are **left-closed and right-open** (`[start, end)`): a value `v` belongs to a bucket
  if `start ≤ v < end`;
- the **last bucket** of each histogram is **left and right closed** (`[start, end]` or `start ≤ v ≤ end`), so that the
  maximum value observed in the interval is always included in some bucket.

The specific type parameter of `Range<T>` depends on the report
(`LocalDateTime`, `Duration` or `Double`).

## 3.4 Custom tests

It is allowed to implement additional custom tests, but these must be placed **exclusively** in the `com.weather.report.test.custom` package. This ensures that such tests can be programmatically excluded from the project's automated evaluation process, preventing them from affecting the grading workflow.  
The folder includes an empty `.gitkeep` file because, being initially empty in the repository, Git would not track that folder otherwise. The `.gitkeep` file is therefore used only to force the directory to be versioned, making it available to place custom test implementations.

# 4. Requirement specifications

## R1 Network

Requirement R1 concerns the management of `Network` and `Operator` entities, together with a reporting part at network level.

### Data import

For this requirement it is necessary to complete the implementation of the `storeMeasurements` method in the `DataImportingService` class:

```
public static void storeMeasurements(String filePath)
```

The method must:

- read measurements from CSV files;
- create the corresponding `Measurement` instances and save them in the database;
- after each save, invoke the private method:

  ```
  private static void checkMeasurement(Measurement measurement)
  ```

#### Threshold violation check

In the `checkMeasurement(Measurement measurement)` method, it is necessary to implement the check of the possible thresholds associated with the sensor that produced the measurement.

In particular, the logic must:

- obtain the sensor corresponding to the measurement;
- verify whether a `Threshold` has been defined for that sensor;
- determine whether the measured value violates the configured threshold;
- in case of violation, invoke the method:

  ```
  AlertingService.notifyThresholdViolation(...)
  ```

  as described in [section 2.5](#25-services) on notification services.

At this point there would theoretically be a direct dependency on the implementation of `Sensor` and `Threshold`.  
However, this dependency is eliminated by the fact that, in the tests, the interaction with `CRUDRepository<Sensor, String>` is _mocked_: mocking consists in replacing the real implementation with a test object that simulates the expected behaviour and returns controlled data.  
For this mechanism to work correctly, it is important to use the sensor referenced by the `currentSensor` variable already present in the method, without modifying the structure of the provided code but adding to it the operational logic for checking the saved value.

### NetworkOperations

The `NetworkOperations` interface groups the functionalities for managing networks and operators, as well as for producing the network report.  
The exposed methods have self-describing names; to understand when to throw the various exceptions it is sufficient to refer to the [section 2.6](#26-exceptions) dedicated to the exception model.

The concrete implementation of `NetworkOperations` must:

- create, update and delete `Network` and `Operator` entities;
- use the metadata inherited from `Timestamped` to record creation and modification information of networks;
- notify the deletion of a `Network` through the call:

  ```
  AlertingService.notifyDeletion(...)
  ```

The method

```
Collection<Network> getNetworks(String... codes)
```

allows obtaining all the `Network` objects whose code is passed in the method’s parameter list. If a code passed as input does not correspond to an element present in the system, it is simply ignored. If the method is invoked without any input parameters, it must return all `Network` elements present in the system.

#### NetworkReport

`NetworkOperations` also exposes the method to obtain a `NetworkReport` starting from the code of a network and an optional time interval.

The `NetworkReport` must contain:

- `networkCode`: the code passed as input;
- `startDate` / `endDate`: the received strings (possibly null);
- `numberOfMeasurements`: total number of measurements of the network in the interval;
- `mostActiveGateways`: gateways with the highest number of measurements;
- `leastActiveGateways`: gateways with the lowest number of measurements;
- `gatewaysLoadRatio`: map `<gatewayCode, ratio>`
  - `ratio` is the percentage of measurements generated by a single gateway with respect to the total of the network.
- `histogram`: map `<Range<LocalDateTime>, count>`.  
  The map groups the network’s measurements into consecutive time buckets whose
  granularity (hourly or daily) depends on the duration of the requested interval or,
  if no interval is provided, on the effective range of available measurements.  
  Each `Range<LocalDateTime>` key contains the exact start and end instants of the
  bucket, together with its unit (`HOUR` or `DAY`).  
  Buckets follow the global histogram convention: they are left-closed and
  right-open `[start, end)`, except for the last bucket, which is `[start, end]`
  so that the maximum timestamp is included.  
  The associated value is the number of measurements whose timestamps fall into
  the bucket according to this convention (`start ≤ t < end`, or
  `start ≤ t ≤ end` for the last bucket).
  The histogram is exposed as a `SortedMap`: buckets fully cover the considered interval and are returned in ascending order
  with respect to their start instant.

### OperationsFactory

To complete requirement R1 it is necessary to:

- provide a concrete implementation of `NetworkOperations`;
- update the `OperationsFactory` class so that the method:

  ```
  public static NetworkOperations getNetworkOperations()
  ```

  returns an instance of this implementation.

## R2 Gateway

Requirement R2 focuses on the management of `Gateway` and their `Parameter`, as well as reporting at gateway level.

### Data import

For R2, it is required to use the `storeMeasurements` method of `DataImportingService`:

```
public static void storeMeasurements(String filePath)
```

The implementation must:

- read measurements from the CSV file;
- save them in the database through the repositories;
- invoke, after each save, the method:

```
checkMeasurement(measurement)
```

even if the body of `checkMeasurement` is not implemented in the branch related to R2.

### GatewayOperations

The `GatewayOperations` interface groups methods that allow:

- creating, updating and deleting gateways;
  - deleting a Gateway also deletes all its associated parameters.
- defining and modifying the parameters associated with gateways;
- producing the gateway report.

The concrete implementation must:

- respect the exception throwing conditions;
- use the `Timestamped` metadata to track creation and modifications of gateways;
- invoke `AlertingService.notifyDeletion(...)` when a gateway is deleted.

The method

```
Collection<Gateway> getGateways(String... codes)
```

allows obtaining all the `Gateway` objects whose code is passed in the method’s parameter list. If a code passed as input does not correspond to an element present in the system, it is simply ignored. If the method is invoked without any input parameters, it must return all `Gateway` elements present in the system.

#### GatewayReport

The `GatewayReport` must contain:

- `code`: the code of the requested Gateway;
- `startDate` / `endDate`: the strings received as input (possibly null);
- `numberOfMeasurements`: total number of measurements of the Gateway in the requested interval;
- `mostActiveSensors`: sensors with the highest number of measurements;
- `leastActiveSensors`: sensors with the lowest number of measurements;
- `sensorsLoadRatio`: map `<sensorCode, ratio>`
  - `ratio` is the percentage of measurements performed by a single sensor with respect to the total of the Gateway;
- `outlierSensors`: list of `sensorCode` whose mean value is flagged as anomalous based on the formula shown below;
- `batteryChargePercentage`: the current value of the Gateway’s `BATTERY_CHARGE` Parameter (it does not depend on the requested time interval).
- `histogram`: map `<Range<Duration>, count>`.  
   Represents the histogram of inter-arrival times between consecutive
  measurements of the gateway within the requested interval.  
   If at least two measurements are available, all inter-arrival durations are
  computed and the resulting range is partitioned into **20 contiguous buckets**
  represented by `Range<Duration>`.  
  Each key identifies a duration interval following the global histogram
  convention: all buckets except the last one are left-closed and right-open
  `[start, end)`, while the last bucket is `[start, end]` so that the maximum
  inter-arrival time is included.  
  The associated value is the number of inter-arrival times whose duration
  falls into that interval.
  All buckets together fully cover the `[minDuration, maxDuration]` interval and
  the map is a `SortedMap` ordered by ascending bucket start duration, so
  iterating over the entries yields the buckets in increasing order of
  inter-arrival time.

### Calculation of Outlier Sensors

A sensor is considered an _outlier_ if its **mean of the measurements** differs from the expected value (`EXPECTED_MEAN`) by **at least two times the expected standard deviation** (`EXPECTED_STD_DEV`).

The formal condition is:

$$
\left| \overline{x}_{\text{sensor}} - \mu_{\text{expected}} \right| \ge 2 \cdot \sigma_{\text{expected}}
$$

Where:

- $\overline{x}_{\text{sensor}}$ is the mean of the sensor’s measurements in the requested time interval;
- $\mu_{\text{expected}}$ is the value of the Gateway’s `EXPECTED_MEAN` parameter;
- $\sigma_{\text{expected}}$ is the value of the Gateway’s `EXPECTED_STD_DEV` parameter.

### OperationsFactory

To complete requirement R2 it is necessary to:

- develop a concrete implementation of `GatewayOperations`;
- modify `OperationsFactory` so that the method:

  ```
  public static GatewayOperations getGatewayOperations()
  ```

  returns the instance of the corresponding implementation.

## R3 Sensor

Requirement R3 concerns the management of `Sensor` and `Threshold`, together with reporting at single sensor level.

### Data import

For R3 it is also necessary to use the `storeMeasurements` method of `DataImportingService`:

```
public static void storeMeasurements(String filePath)
```

The implementation must:

- read measurements from CSV files;
- save them in the database using the repositories;
- call `checkMeasurement(measurement)` after each insertion.

It is sufficient that the call to `checkMeasurement` is present at the correct point, even if the internal logic of the method is not implemented in the branch dedicated to R3.

### SensorOperations

The `SensorOperations` interface contains methods to:

- create, update and delete sensors;
  - deleting a Sensor also deletes any associated threshold.
- manage thresholds associated with sensors;
- retrieve sensors based on their codes;
- obtain the report for a specific sensor.

The implementation must:

- apply the validation rules and exception throwing conditions described in [section 2.6](#26-exceptions);
- use `Timestamped` to record creation and modification information of sensors;
- invoke `AlertingService.notifyDeletion(...)` when a sensor is deleted.

The method

```
Collection<Sensor> getSensors(String... codes)
```

allows obtaining all the `Sensor` objects whose code is passed in the method’s parameter list. If a code passed as input does not correspond to an element present in the system, it is simply ignored. If the method is invoked without any input parameters, it must return all `Sensor` elements present in the system.

#### SensorReport

The `SensorReport` contains the following information:

- `code`: code of the requested Sensor;
- `startDate`, `endDate`: the dates received as input (may be null);
- `numberOfMeasurements`: total number of measurements recorded in the interval;
- `mean`: mean of the measured values;
- `variance`: variance of the measurements;
- `stdDev`: standard deviation;
- `minimumMeasuredValue`: minimum recorded value;
- `maximumMeasuredValue`: maximum recorded value;
- `outliers`: measurements considered outliers.
- `histogram`: map `<Range<Double>, count>`.  
   The map represents a histogram of the values measured by the sensor in the considered interval, built using **only** the measurements that are not classified as outliers.
  The system determines the minimum and maximum value among the non-outlier
  measurements and partitions this span into **20 equal-width intervals**, each represented by a `Range<Double>` whose bounds are left-inclusive and right-exclusive, except for the last interval, which is `[start, end]` so that the maximum observed value is included.  
  The value associated with each interval is the number of non-outlier measurements whose value falls into that interval according to this convention (`[start ≤ v < end`, or `start ≤ v ≤ end` for the last interval).
  The same binning strategy (20 equal-width bins over the [min, max] span of non-outlier values) must be applied consistently for all sensors, although the actual interval boundaries depend on the values observed for the specific sensor.  
  If no non-outlier measurements exist within the report interval, the histogram may be empty.
  The histogram is exposed as a `SortedMap`: buckets are ordered by their start
  value, so iterating over the map returns the intervals in increasing order of measurement value.

### OperationsFactory

To satisfy requirement R3 it is necessary to:

- concretely implement the `SensorOperations` interface;
- update `OperationsFactory` so that the method:

  ```
  public static SensorOperations getSensorOperations()
  ```

  returns the correct instance.

## R4 Topology

Requirement R4 is an integration requirement and concerns the management of the system topology, i.e. the relationships between `Network`, `Gateway` and `Sensor`.

The branch dedicated to R4 must be created from `main`, after the implementations of requirements R1, R2 and R3 have been integrated.  
On this branch it is possible to:

- perform code refactoring;
- harmonise design choices;
- complete functionalities that require cooperation between the different parts of the system.

### Refactoring

In this phase it is possible to:

- remove duplications;
- reorganise the code in a clearer way;
- centralise common logic introduced in the individual requirements;
- improve the overall readability and maintainability of the project.

Refactoring must preserve public interfaces and the correct functioning of existing tests.

### TopologyOperations

The `TopologyOperations` interface defines the operations related to the relationships between the main entities, for example:

- associate or disassociate a gateway with a network;
- associate or disassociate a sensor with a gateway;
- obtain the list of gateways associated with a network;
- obtain the list of sensors associated with a gateway.

The concrete implementation of this interface must apply the same validation and exception handling rules described previously.

### OperationsFactory

To complete requirement R4 it is finally necessary to:

- provide an implementation of `TopologyOperations`;
- update `OperationsFactory` so that the method:

  ```
  public static TopologyOperations getTopologyOperations()
  ```

  returns the corresponding implementation.

In this way the `WeatherReport` object will also be able to expose topology management functionalities, integrating in a single access point all the operations developed in requirements R1, R2, R3 and R4.
