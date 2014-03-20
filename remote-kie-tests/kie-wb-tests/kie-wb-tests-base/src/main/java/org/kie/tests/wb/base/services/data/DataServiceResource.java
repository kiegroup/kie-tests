package org.kie.tests.wb.base.services.data;

import static javax.ws.rs.core.HttpHeaders.*;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Variant;

import org.jboss.resteasy.core.request.ServerDrivenNegotiation;
import org.jbpm.console.ng.bd.service.DataServiceEntryPoint;
import org.jbpm.console.ng.pr.model.ProcessInstanceSummary;


@Path("/data")
@RequestScoped
public class DataServiceResource {

    @Inject
    private DataServiceEntryPoint dataServices;
   
    @Context
    private HttpHeaders headers;
    
    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("/process/instance/{procInstId: [0-9]+}")
    public Response getProcessInstanceSummary(@PathParam("procInstId") Long procInstId) {
        ProcessInstanceSummary summary = dataServices.getProcessInstanceById(procInstId);
        
        return createCorrectVariant(new JaxbProcessInstanceSummary(summary), headers, Status.OK);
    }
 
    private static List<Variant> variants 
    = Variant.mediaTypes(MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE).add().build();
    private static Variant defaultVariant 
    = Variant.mediaTypes(MediaType.APPLICATION_XML_TYPE).add().build().get(0);

    protected static Response createCorrectVariant(Object responseObj, HttpHeaders headers, javax.ws.rs.core.Response.Status status) { 
        ResponseBuilder responseBuilder = null;
        Variant v = getVariant(headers);
        if( v == null ) { 
            v = defaultVariant;
        }
        if( status != null ) { 
            responseBuilder = Response.status(status).entity(responseObj).variant(v);
        } else { 
            responseBuilder = Response.ok(responseObj, v);
        }
        return responseBuilder.build();
    }
    
    public static Variant getVariant(HttpHeaders headers) { 
        // copied (except for the acceptHeaders fix) from RestEasy's RequestImpl class
        ServerDrivenNegotiation negotiation = new ServerDrivenNegotiation();
        MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
        List<String> acceptHeaders = requestHeaders.get(ACCEPT);
        // Fix
        if( acceptHeaders != null && ! acceptHeaders.isEmpty() ) { 
            List<String> fixedAcceptHeaders = new ArrayList<String>();
            for(String header : acceptHeaders ) { 
                fixedAcceptHeaders.add(header.replaceAll("q=\\.", "q=0.")); 
            }
            acceptHeaders = fixedAcceptHeaders;
        }
        negotiation.setAcceptHeaders(acceptHeaders);
        negotiation.setAcceptCharsetHeaders(requestHeaders.get(ACCEPT_CHARSET));
        negotiation.setAcceptEncodingHeaders(requestHeaders.get(ACCEPT_ENCODING));
        negotiation.setAcceptLanguageHeaders(requestHeaders.get(ACCEPT_LANGUAGE));

        return negotiation.getBestMatch(variants);
        // ** use below instead of above when RESTEASY-960 is fixed **
        // return restRequest.selectVariant(variants); 
    }
}
