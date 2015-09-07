package org.kie.tests;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;

@XmlRootElement(name="my-type")
@XmlAccessorType(XmlAccessType.FIELD)
public class MyType implements Serializable {

    /**
     * Default ID.
     */
    private static final long serialVersionUID = 1L;
    
    @XmlElement
    @XmlSchemaType(name="string")
    private String text;
    
    @XmlElement
    @XmlSchemaType(name="int") 
    private Integer data;
    
    public MyType() {
       // default constructor 
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        MyType other = (MyType) obj;
        if( data == null ) {
            if( other.data != null )
                return false;
        } else if( !data.equals(other.data) )
            return false;
        if( text == null ) {
            if( other.text != null )
                return false;
        } else if( !text.equals(other.text) )
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "MyType{" + "text=" + text + ", data=" + data + "}";
    }
}
