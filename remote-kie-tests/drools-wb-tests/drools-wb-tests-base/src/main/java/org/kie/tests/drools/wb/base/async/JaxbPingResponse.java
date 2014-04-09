package org.kie.tests.drools.wb.base.async;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="pong")
@XmlAccessorType(XmlAccessType.FIELD)
public class JaxbPingResponse {

    @XmlElement
    public Integer id;
    
    @XmlElement
    public String status;
 
    public JaxbPingResponse() { 
        // default for JAXB
    }
    
    public JaxbPingResponse(int id, String status) { 
        this.id = id;
        this.status = status;
    }
    
}
