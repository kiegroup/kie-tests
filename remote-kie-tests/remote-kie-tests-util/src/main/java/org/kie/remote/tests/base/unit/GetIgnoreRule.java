package org.kie.remote.tests.base.unit;

/*******************************************************************************
 * Copyright (c) 2013,2014 RÃ¼diger Herrmann
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * RÃ¼diger Herrmann - initial API and implementation
 * Matt Morrissette - allow to use non-static inner IgnoreConditions
 ******************************************************************************/

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.conn.HttpHostConnectException;
import org.junit.Assume;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class GetIgnoreRule implements MethodRule {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    public @interface IgnoreIfGETFails {
        String getUrl() default "";
    }

    @Override
    public Statement apply( Statement base, FrameworkMethod method, Object target ) {
        Statement result = base;
        if( hasConditionalIgnoreAnnotation(method) ) {
            String urlString = getGetUrl(target, method);
            String message = "Ignored because [GET] " + urlString + " failed.";
            boolean liveServer = false;
            try {
                URL getUrl = new URL(urlString);
                liveServer = true;
            } catch( MalformedURLException e ) {
                liveServer = false;
                message = "Ignored because [" + urlString + "] is not a valid URL.";
            }
            if( liveServer ) {
                try {
                    Response response = Request.Get(urlString).execute();
                    int code = response.returnResponse().getStatusLine().getStatusCode();
                    if( code > 401 ) {
                        liveServer = false;
                        message = "Ignored because [GET] " + urlString + " returned " + code;
                    }
                } catch( HttpHostConnectException hhce ) {
                    liveServer = false;
                    message = "Ignored because server is not available: " + hhce.getMessage(); 
                } catch( Exception e ) {
                    liveServer = false;
                    message = "Ignored because [GET] " + urlString + " threw: " + e.getMessage();
                }
            }
            if( !liveServer ) {
                result = new IgnoreStatement(message);
            }
        }
        return result;
    }

    private boolean hasConditionalIgnoreAnnotation( FrameworkMethod method ) {
        return method.getAnnotation(IgnoreIfGETFails.class) != null;
    }

    private String getGetUrl( Object instance, FrameworkMethod method ) {
        IgnoreIfGETFails annotation = method.getAnnotation(IgnoreIfGETFails.class);
        return annotation.getUrl();
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