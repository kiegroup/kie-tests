package org.kie.tests.wb.base.services.data;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Path("/class")
@RequestScoped
public class ClassLoaderResource {

    private static final Logger logger = LoggerFactory.getLogger(ClassLoaderResource.class);
    
    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("/load/{class: [a-zA-Z0-9\\.]+}")
    public void load(@PathParam("class") String className) {
        String out = null;
        try {
            out = Class.forName(className).getName();
            logger.info( "YAY! : " + out );
        } catch (ClassNotFoundException cnfe) {
            logger.error( "NOO! : " + cnfe.getMessage(), cnfe );
        }
    }
    
}
