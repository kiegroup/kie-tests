kie-tests
=========

Repository for integration tests and other tests involving multiple projects. 

In general, when adding modules (at any level) to this repo, try to keep the following structure in mind: 

- Root level modules should be named after what they're testing: 
  - "kie-wb-tests"
  - "jbpm-tests"
  - "dashboard-tests"
  and etc. 

- Nested modules should be named after the specific types of tests that they are: 
  - "kie-wb-tests/kie-wb-tests-jboss"
  - "jbpm-tests/jbpm-concurrency-tests"
  and etc.. 
  
Thanks! 

Developing Drools and jBPM
==========================

**If you want to build or contribute to a droolsjbpm project, [read this document](https://github.com/droolsjbpm/droolsjbpm-build-bootstrap/blob/master/README.md).**

**It will save you and us a lot of time by setting up your development environment correctly.**
It solves all known pitfalls that can disrupt your development.
It also describes all guidelines, tips and tricks.
If you want your pull requests (or patches) to be merged into master, please respect those guidelines.
