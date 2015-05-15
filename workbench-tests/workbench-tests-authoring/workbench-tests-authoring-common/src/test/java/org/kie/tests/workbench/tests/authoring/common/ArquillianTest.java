/*
 * Copyright 2015 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.tests.workbench.tests.authoring.common;

import javax.inject.Inject;

import org.drools.workbench.screens.drltext.client.handlers.NewDrlTextHandler;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.widgets.client.handlers.NewResourcePresenter;

@RunWith(Arquillian.class)
public class ArquillianTest {

    @Deployment
    public static JavaArchive createDeployment() {
        // FIXME the jar is currently empty!!
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class)
                .merge(ShrinkWrap.create(JavaArchive.class, "target/test-libs/kie-wb-common-ui.jar"))
                .merge(ShrinkWrap.create(JavaArchive.class, "target/test-libs/drools-wb-drl-text-editor-client.jar"));
        System.out.println(jar.toString());
        System.out.println(jar.toString(true));
        return jar;
    }
    @Inject
    private NewResourcePresenter p;

    @Test
    public void should_create_greeting() {
        p.show(new NewDrlTextHandler());
    }
}
