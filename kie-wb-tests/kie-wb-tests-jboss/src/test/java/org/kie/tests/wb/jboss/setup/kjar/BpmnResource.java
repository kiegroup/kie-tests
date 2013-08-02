package org.kie.tests.wb.jboss.setup.kjar;

public class BpmnResource { 
    public String fileName;
    public String path;
    public BpmnResource( String fileNname, String path ) { 
        this.fileName = fileNname;
        this.path = path;
    }
    
    public BpmnResource( String path ) { 
        this.path = path;
        this.fileName = path.replaceAll("^.*/", "");
    }
}