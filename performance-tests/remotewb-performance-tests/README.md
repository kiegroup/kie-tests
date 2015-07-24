# Remote Workbench Performance Tests
This module uses kie-performance-kit as a basis for the remote workbench scenarios kept in org.kie.perf.scenario.

The tests should be executed via `run.sh` bash script since this script is by default used for running selected test suite when no scenario is selected.

1. Edit `performance.properties` located in *src/main/resources*
2. [optional] Setup JVM in `pom.xml` by adding arguments into *exec-maven-plugin*
3. Execute `run.sh` or manually *mvn clean install exec:exec -Dscenario=[scenario]*

## JBoss EAP 6 Setup

1. Make sure that workbench is located at http://localhost:8080/business-central (this location is used by default in the `performance.properties` and the scripts `addUsers.sh` and `deploy.sh`)
2. Execute `addUsers.sh` to add perfUser and engUser
3. Start the application server with deployed workbench
4. Execute `deploy.sh` in workbench-assets module to build and deploy the kjar containing business processes and business rules for performance testing

## KIE-Performance-Kit Setup

All configuration goes into `performance.properties` located in *src/main/resources*.

* Select suite and scenario
 * Available suites = `LoadSuite, ConcurrentLoadSuite`
 * To run the whole suite, comment scenario property to be null and make sure that `startScriptLocation` is set correctly.
* Select run type of the test suite
 * Available run types - `Duration, Iteration`
 * If Duration run type is chose, every scenario will run given given time according to time set into `duration` property (measured in seconds).
 * If Iteration run type is chose, every scenario will run given number of iterations according to number set into `iterations` property.
* Turn ON/OFF warm up before scenario
 * To enable warm up, set true to `warmUp` property.
 * Every scenario will run X times according to `warmUpCount` property
* Running scenarios concurrently in threads
 * When ConcurrentLoadSuite is used we are able to set number threads in which the test suites should run.
 * The total number of tests = `threads` * iterations
* Reporting
 * Types of reporters = Console, CSVSingle, CSV
 * **Console**
 * **CSV** - reports periodiacally after X seconds into CSV files for every metrics
 * **CSVSingle** - reports scenario CSV files containing the metrics results
* Additional metrics
 * **MemoryUsage** - HEAP and pools usage (Eden, Old Gen, etc.)
 * **ThreadStates** - number of threads and their states (waited, runnable, new, blocked, etc.), deadlocks, ...
 * **FileDescriptors** - number of opened descriptors, percentage of usage
 * Set any of the metrics above as a list into *measure* property

## Remote Workbench KIE-Performance-Kit Setup

* remoteAPI = `REST` or `JMS` (it's not possible to test both at once)
* username - admin application user for most of the performance tests
* host - if different to `localhost` it has to be changed in the scripts (`addUser.sh` and `deploy.sh`) and in distribution management of workbench-assets' `pom.xml` too
* sslEnabled - if SSL is enabled it has to be configured on the application server and keystore information provided!

