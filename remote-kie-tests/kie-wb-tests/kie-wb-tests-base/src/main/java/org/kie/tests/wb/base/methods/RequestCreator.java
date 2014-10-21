package org.kie.tests.wb.base.methods;

import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.kie.remote.common.rest.KieRemoteHttpRequest;

class RequestCreator {

    private final URL baseUrl;
    private final String userName;
    private final String password;
    private final MediaType contentType;

    public RequestCreator(URL baseUrl, String user, String password, MediaType mediaType) {
        StringBuilder urlString = new StringBuilder(baseUrl.toString());
        if( !urlString.toString().endsWith("/") ) {
            urlString.append("/");
        }
        urlString.append("rest/");
        try {
            this.baseUrl = new URL(urlString.toString());
        } catch(Exception e) { 
            e.printStackTrace();
            throw new IllegalStateException("Invalid url: " +  urlString, e);
        }
        this.userName = user;
        this.password = password;
        this.contentType = mediaType;
    }

    public KieRemoteHttpRequest createRequest( String relativeUrl ) {
        KieRemoteHttpRequest request = KieRemoteHttpRequest.newRequest(baseUrl).basicAuthorization(userName, password)
                .relativeRequest(relativeUrl).accept(contentType.toString());
        return request;
    }
}