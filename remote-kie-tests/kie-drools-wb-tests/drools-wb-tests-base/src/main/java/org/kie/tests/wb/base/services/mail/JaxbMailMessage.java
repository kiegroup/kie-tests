package org.kie.tests.wb.base.services.mail;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;

@XmlRootElement(name="process-instance-summary")
@XmlAccessorType(XmlAccessType.FIELD)
public class JaxbMailMessage {
    
    @XmlElement
    @XmlSchemaType(name="string")
    private String from;
    
    @XmlElement
    @XmlSchemaType(name="string")
    private String to;
    
    @XmlElement
    @XmlSchemaType(name="string")
    private String body;
    
    public JaxbMailMessage(String sender, String receiver, String content) {
        this.from = sender;
        this.to = receiver;
        this.body = content;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }


}
