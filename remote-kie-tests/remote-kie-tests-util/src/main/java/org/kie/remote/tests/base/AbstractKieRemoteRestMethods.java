package org.kie.remote.tests.base;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.kie.remote.common.rest.KieRemoteHttpRequest;
import org.kie.remote.common.rest.KieRemoteHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractKieRemoteRestMethods {

    private static Logger logger = LoggerFactory.getLogger(AbstractKieRemoteRestMethods.class);

    public abstract String getContentType();

    public abstract <T> T deserializeXml( String xmlStr, Class<T> entityType );

    public abstract <T> T deserializeJson( String jsonStr, Class<T> entityType );

    protected <T> T noXmlContent(Object testClass, Class<T> entityType) { 
        fail( "There should be no XML content returned in HTTP requests done in " + testClass.getClass().getSimpleName() + "." );     
        return (T) null;
    }
    
    protected <T> T deserialize( KieRemoteHttpResponse httpResponse, Class<T> entityClass ) {
        String acceptHeader = httpResponse.header(HttpHeaders.CONTENT_TYPE);
        T result = null;
        assertNotNull("Null accept header in response.", acceptHeader);
        if( MediaType.APPLICATION_XML.equals(acceptHeader) ) {
            result = deserializeXml(httpResponse.body(), entityClass);
        } else if( MediaType.APPLICATION_JSON.equals(acceptHeader) ) {
            result = deserializeJson(httpResponse.body(), entityClass);
        } else {
            fail("Unknown content type header in response: " + acceptHeader);
        }
        return result;
    }

    protected void addToRequestBody( KieRemoteHttpRequest httpRequest, Object entity ) {
        httpRequest.accept(getContentType());
        String body = null;
        String contentType = getContentType();
        if( MediaType.APPLICATION_XML.equals(contentType) ) {
            body = serializeToXml(entity);
        } else if( MediaType.APPLICATION_JSON.equals(contentType) ) {
            body = serializeToJson(entity);
        } else {
            fail("Unknown content type header in response: " + contentType);
        }
        logger.debug( "]] " + body );
        httpRequest.body(body);
    }
   
    protected String serializeToXml(Object entity) { 
       fail("The " + this.getClass().getSimpleName() + "." + Thread.currentThread().getStackTrace()[0].getMethodName() 
               + " method needs to be overriden in the local test!");
       return null;
    }
    
    protected String serializeToJson(Object entity) { 
       fail("The " + this.getClass().getSimpleName() + "." + Thread.currentThread().getStackTrace()[0].getMethodName() 
               + " method needs to be overriden in the local test!");
       return null;
    }
    
    protected void checkResponse( KieRemoteHttpRequest httpRequest, int status ) {
        KieRemoteHttpResponse httpResponse = httpRequest.response();
        if( status != httpResponse.code() ) {
            String content = httpResponse.header(HttpHeaders.CONTENT_TYPE);
            String body = httpResponse.body();
            if( content.startsWith(MediaType.TEXT_HTML) ) { 
                Document doc = Jsoup.parse(body);
                body = doc.body().text();
            }
            logger.warn("Response with exception:\n" + body);
            fail("Incorrect status: " + httpResponse.code() + " (" + httpRequest.getUri() + ")");
        }
    }

    protected <T> T get( KieRemoteHttpRequest httpRequest, Class<T> entityType ) {
        logger.debug( "> [GET] " + httpRequest.getUri().toString() );
        KieRemoteHttpResponse httpResponse = httpRequest.accept(getContentType()).get().response();
        checkResponse(httpRequest, 200);
        try {
            return deserialize(httpResponse, entityType);
        } finally {
            httpRequest.disconnect();
        }
    }

    protected void get( KieRemoteHttpRequest httpRequest ) {
        logger.debug( "> [GET] " + httpRequest.getUri().toString() );
        httpRequest.accept(getContentType()).get();
        checkResponse(httpRequest, 200);
        httpRequest.disconnect();
    }

    protected <T> T post( KieRemoteHttpRequest httpRequest, int status, Class<T> entityType ) {
        return post(httpRequest, status, entityType, Integer.MAX_VALUE);
    }
    
    protected <T> T post( KieRemoteHttpRequest httpRequest, int status, Class<T> entityType, int timeout ) {
        logger.debug( "> [POST] " + httpRequest.getUri().toString() );
        long after, before = System.currentTimeMillis();
        KieRemoteHttpResponse httpResponse = httpRequest.accept(getContentType()).post().response();
        after = System.currentTimeMillis();
        long duration = after - before;
        assertTrue("Post call took longer than " + timeout + " ms: " + duration, duration <= timeout);
        checkResponse(httpRequest, status);
        try {
            return deserialize(httpResponse, entityType);
        } finally {
            httpRequest.disconnect();
        }
    }

    protected String post( KieRemoteHttpRequest httpRequest, int status ) {
        logger.debug( "> [POST] " + httpRequest.getUri().toString() );
        httpRequest.accept(getContentType()).post();
        checkResponse(httpRequest, status);
        String result = httpRequest.response().body();
        httpRequest.disconnect();
        return result;
    }

    protected <T> T delete( KieRemoteHttpRequest httpRequest, int status, Class<T> entityClass ) {
        logger.debug( "> [DELETE] " + httpRequest.getUri().toString() );
        KieRemoteHttpResponse httpResponse = httpRequest.accept(getContentType()).delete().response();
        checkResponse(httpRequest, status);
        try {
            return deserialize(httpResponse, entityClass);
        } finally {
            httpRequest.disconnect();
        }
    }

}
