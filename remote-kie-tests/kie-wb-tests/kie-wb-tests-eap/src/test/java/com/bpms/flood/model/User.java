package com.bpms.flood.model;

import java.io.Serializable;

public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer age;

    private String name;

    public User() {
        
    }

    public User(String name, Integer age) {
        this.name = name;
        this.age = age;
    }

    public Integer getAge() {
        return this.age;
    }

    public void setAge(Integer age ) {
        this.age = age;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name ) {
        this.name = name;
    }

    public String toString() {
        return "[Name="+name+", age="+age+"]";
    }

}