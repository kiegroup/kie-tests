kie-tests
=========

Repository for tests involving multiple projects as well as tests that might not fit into specific
modules. Please feel free to add any tests!

In general, when adding modules (at any level) to this repo, try to keep the following structure 
in mind: 

- Root level modules should be named after what they're testing. For example:  
  - "remote-kie-tests"
  - "core-kie-tests"

- Nested modules should be named after the specific types of tests that they include. For example:
  - "core-kie-tests/core-marshalling-kie-tests/jbpm-marshalling-kie-tests"

The format here is: `<type test>`-kie-tests. 

- However, if the tests are aimed at a specific war or application, please name them after the
  application, followed by the platform that they test. For example:   
  - "remote-kie-tests/drools-wb-tests/drools-wb-tests-jboss"

Using this naming system ('nomenclature') means it's easier for other developers to quickly 
find existing tests or otherwise create new tests or modules in the appropriate place. 

Thanks!

Developing Drools and jBPM
==========================

**If you want to build or contribute to a droolsjbpm project, [read this document](https://github.com/droolsjbpm/droolsjbpm-build-bootstrap/blob/master/README.md).**

**It will save you and us a lot of time by setting up your development environment correctly.**
It solves all known pitfalls that can disrupt your development.
It also describes all guidelines, tips and tricks.
If you want your pull requests (or patches) to be merged into master, please respect those guidelines.
