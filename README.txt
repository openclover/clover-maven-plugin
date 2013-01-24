Clover-for-Maven2&3 Plugin is a Clover integration with Maven 2.x / 3.x.

This project is open-source, based on the Apache License version 2.0.

Documentation: https://confluence.atlassian.com/display/CLOVER

Issue tracker: https://jira.atlassian.com/browse/CLOV ("Maven Plugin" component)

Clover home page: http://www.atlassian.com/software/clover/

Previous project site was here: https://studio.plugins.atlassian.com/browse/CLMVN

Developer documentation: https://confluence.atlassian.com/x/zoFyCw

===================================================

To run the integration tests you need a clover.license file. You can define it in ~/.m2/settings.xml, for instance:

<properties>
    <clover.licenseLocation>file:///path/to/the/clover.license</clover.licenseLocation>
<properties>

Useful Maven targets:

mvn integration-test -Pintegration-tests # runs the integration tests
