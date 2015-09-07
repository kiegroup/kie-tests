/*
 * JBoss, Home of Professional Open Source
 * 
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.remote.tests.base.unit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Properties;

import org.junit.Assume;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class MavenBuildIgnoreRule implements MethodRule {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    public @interface IgnoreWhenInMavenBuild {
    }

    @Override
    public Statement apply( Statement base, FrameworkMethod method, Object target ) {
        Statement result = base;
        if( hasConditionalIgnoreAnnotation(method) ) {
            String message = "Ignored because run in Maven build";
        
            StackTraceElement [] ste = Thread.currentThread().getStackTrace();
            for( int i = 0; i < 10; ++i ) { 
               if( ste[i].getClassName().contains("org.apache.maven") )  { 
                  return new IgnoreStatement(message);
               }
            }
            Properties props = System.getProperties();
            for( Object propKey : props.keySet() ) { 
               if( propKey.toString().startsWith("surefire") )  { 
                  return new IgnoreStatement(message);
               }
            }
        }
        return result;
    }

    private boolean hasConditionalIgnoreAnnotation( FrameworkMethod method ) {
        return method.getAnnotation(IgnoreWhenInMavenBuild.class) != null;
    }

    private static class IgnoreStatement extends Statement {
        private final String message;

        IgnoreStatement(String host) {
            this.message = host;
        }

        @Override
        public void evaluate() {
            Assume.assumeTrue(message, false);
        }
    }

}