package org.kie.remote.tests.base;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.kie.remote.tests.base.unit.MavenBuildIgnoreRule;
import org.kie.remote.tests.base.unit.MavenBuildIgnoreRule.IgnoreWhenInMavenBuild;

public class IgnoreWhenInMavenBuildTest {

    @Rule
    public MavenBuildIgnoreRule ignoreRule = new MavenBuildIgnoreRule();
    
    @Test
    @IgnoreWhenInMavenBuild
    public void mavenBuildTest() { 
       
       Assert.fail("This test should never have been run in a maven build!");
    }
}
