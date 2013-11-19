package org.kie.tests.wb.base.test;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class MyType {

    @XmlAttribute(required = true)
    private String text;
    
    @XmlAttribute
    private Integer data;
    
    public MyType() {
        
    }
    
    public MyType(String text, int data) {
        this.text = text;
        this.data = data;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }

    public Integer getData() {
        return data;
    }
    
    public void setData(Integer data) {
        this.data = data;
    }
    
}
