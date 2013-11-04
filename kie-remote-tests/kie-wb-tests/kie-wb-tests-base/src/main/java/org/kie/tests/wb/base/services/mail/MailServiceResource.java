package org.kie.tests.wb.base.services.mail;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


@Path("/mail")
@RequestScoped
public class MailServiceResource {

    @Inject
    private TestSMTPServerService smtpServerService;
    
    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("/messages" )
    public JaxbMailMessageList getMessages() { 
        List<JaxbMailMessage> msgList = smtpServerService.getMessages();
        return new JaxbMailMessageList(msgList);
    }
    
}
