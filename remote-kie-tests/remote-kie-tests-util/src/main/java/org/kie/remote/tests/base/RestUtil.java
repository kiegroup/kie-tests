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
package org.kie.remote.tests.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.util.HttpHeaderNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestUtil {

    private static Logger logger = LoggerFactory.getLogger(RestUtil.class);

    private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    public static long restCallDurationLimit = 500;

    public static <T extends Object> T getResponseEntity(ClientResponse<?> responseObj, Class<T> responseType ) { 
        T responseEntity = null;
        try { 
            responseEntity = responseObj.getEntity(responseType);
        } catch( Exception e ) { 
            String msg = "Unable to serialize " + responseType.getSimpleName() + " instance";
            responseObj.resetStream();
            logger.error("{}:\n {}", msg, responseObj.getEntity(String.class), e);
            fail(msg);
            throw new RuntimeException("Fail should keep this exception from being thrown!");
        }
        return responseEntity;
    }
    
    public static ClientResponse<?> checkTimeResponse(ClientResponse<?> responseObj) throws Exception {
        long start = System.currentTimeMillis();
        try { 
            return checkResponse(responseObj, 202); 
        } finally { 
           long duration = System.currentTimeMillis() - start;
           assertTrue( "Rest call took too long: " + duration + "ms", duration < restCallDurationLimit);
           logger.info("Op time : " + sdf.format(new Date(duration)));
        }
    }
    
    public static ClientResponse<?> checkResponse(ClientResponse<?> responseObj) throws Exception {
        return checkResponse(responseObj, 200);
    }

    public static ClientResponse<?> checkResponse(ClientResponse<?> responseObj, int status) throws Exception {
        responseObj.resetStream();
        int reqStatus = responseObj.getStatus();
        if (reqStatus != status) {
            logger.warn("Response with exception:\n" + responseObj.getEntity(String.class));
            fail("Incorrect status: " + reqStatus);
        }
        String contentType = (String) responseObj.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if( contentType != null ) { 
            if( ! (contentType.startsWith(MediaType.APPLICATION_XML)) && ! (contentType.startsWith(MediaType.APPLICATION_JSON)) ) { 
               logger.warn("Incorrect format for response: " + contentType + "\n" + responseObj.getEntity(String.class) );
               fail("Incorrect response media type: " + contentType );
            }
        }
        return responseObj;
    }

    public static <T> T  delete(ClientRequest restRequest, MediaType mediaType, Class<T> responseType) throws Exception {
        setAcceptHeader(restRequest, mediaType);
        logger.debug(">> [GET " + restRequest.getHeaders().getFirst(HttpHeaderNames.ACCEPT) + "] " + restRequest.getUri());
        ClientResponse<?> responseObj = checkResponse(restRequest.delete());
        return getResponseEntity(responseObj, responseType);
    }

    public static <T> T get(ClientRequest restRequest, MediaType mediaType, Class<T> responseType) throws Exception {
        setAcceptHeader(restRequest, mediaType);
        logger.debug(">> [GET " + restRequest.getHeaders().getFirst(HttpHeaderNames.ACCEPT) + "] " + restRequest.getUri());
        ClientResponse<?> responseObj = checkResponse(restRequest.get());
        return getResponseEntity(responseObj, responseType);
    }

    public static <T> T post(ClientRequest restRequest, MediaType mediaType, Class<T> responseType) throws Exception {
        setAcceptHeader(restRequest, mediaType);
        logger.debug(">> [POST " + restRequest.getHeaders().getFirst(HttpHeaderNames.ACCEPT) + "] " + restRequest.getUri());
        ClientResponse<?> responseObj = checkResponse(restRequest.post());
        return getResponseEntity(responseObj, responseType);
    }
    
    public static ClientResponse<?> delete(ClientRequest restRequest, MediaType mediaType) throws Exception {
        setAcceptHeader(restRequest, mediaType);
        logger.debug(">> [GET " + restRequest.getHeaders().getFirst(HttpHeaderNames.ACCEPT) + "] " + restRequest.getUri());
        return checkResponse(restRequest.delete());
    }

    public static ClientResponse<?> get(ClientRequest restRequest, MediaType mediaType) throws Exception {
        setAcceptHeader(restRequest, mediaType);
        logger.debug(">> [GET " + restRequest.getHeaders().getFirst(HttpHeaderNames.ACCEPT) + "] " + restRequest.getUri());
        return checkResponse(restRequest.get());
    }

    public static ClientResponse<?> post(ClientRequest restRequest, MediaType mediaType) throws Exception {
        setAcceptHeader(restRequest, mediaType);
        logger.debug(">> [POST " + restRequest.getHeaders().getFirst(HttpHeaderNames.ACCEPT) + "] " + restRequest.getUri());
        return checkResponse(restRequest.post());
    }
    
    public static void setAcceptHeader(ClientRequest restRequest, MediaType mediaType) { 
        assertNotNull( "Null media type.", mediaType );
        MultivaluedMap<String, String> headers = restRequest.getHeaders();
        headers.putSingle(HttpHeaderNames.ACCEPT, mediaType.getType() + "/" + mediaType.getSubtype());
        assertNotNull( "Null ACCEPT headers!", headers.get(HttpHeaderNames.ACCEPT));
        assertEquals( "Multiple ACCEPT headers!", 1, headers.get(HttpHeaderNames.ACCEPT).size());
    }

    public static ClientResponse<?> checkResponsePostTime(ClientRequest restRequest, MediaType mediaType, int status) throws Exception {
        setAcceptHeader(restRequest, mediaType);
        long before, after;
        logger.debug("BEFORE: " + sdf.format((before = System.currentTimeMillis())));
        ClientResponse<?> responseObj = checkResponse(restRequest.post(), status);
        logger.debug("AFTER: " + sdf.format((after = System.currentTimeMillis())));
        long duration = (after - before);
        assertTrue("Call took longer than " + restCallDurationLimit / 1000 + " seconds: " + duration + "ms", duration < restCallDurationLimit);
        return responseObj;
    }
   
}