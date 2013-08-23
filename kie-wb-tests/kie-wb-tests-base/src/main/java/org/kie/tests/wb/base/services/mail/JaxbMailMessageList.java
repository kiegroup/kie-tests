package org.kie.tests.wb.base.services.mail;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.jbpm.process.audit.xml.JaxbProcessInstanceLog;

@XmlRootElement(name = "mail-message-list")
@XmlAccessorType(XmlAccessType.FIELD)
public class JaxbMailMessageList {

    @XmlElements({ @XmlElement(name = "mail-message", type = JaxbProcessInstanceLog.class), })
    List<JaxbMailMessage> mailMessageList;
    
    public JaxbMailMessageList( List<JaxbMailMessage> messageList ) { 
        this.mailMessageList = messageList;
    }

    public List<JaxbMailMessage> getMailMessageList() {
        return mailMessageList;
    }

    public void setMailMessageList(List<JaxbMailMessage> mailMessageList) {
        this.mailMessageList = mailMessageList;
    }
}
