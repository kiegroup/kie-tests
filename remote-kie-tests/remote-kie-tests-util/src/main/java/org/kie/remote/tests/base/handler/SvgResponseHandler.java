package org.kie.remote.tests.base.handler;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.http.entity.ContentType;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

@SuppressWarnings("unchecked")
public class SvgResponseHandler<T,P> extends AbstractResponseHandler<String, P> {

    public SvgResponseHandler(int status) {
        super(ContentType.APPLICATION_SVG_XML, status, String.class);
    }

    public SvgResponseHandler(Class<String>... returnTypes) {
        super(ContentType.APPLICATION_SVG_XML, returnTypes);
    }

    @Override
    protected String deserialize(String content) {

        if( logger.isTraceEnabled() ) {
            try {
                Document doc = DocumentHelper.parseText(content);
                StringWriter sw = new StringWriter();
                OutputFormat format = OutputFormat.createPrettyPrint();
                XMLWriter xw = new XMLWriter(sw, format);
                xw.write(doc);
                String prettyContent = sw.toString();
                logger.trace("SVG XML  < |\n{}", prettyContent );
            } catch( IOException ioe ) {
                logger.error( "Unabel to write XML document: " + ioe.getMessage(), ioe );
            } catch( DocumentException de ) {
                logger.error( "Unabel to parse text: " + de.getMessage(), de );
            }
        }

        return new String(content.getBytes());
    }

    @Override
    public String serialize( Object entity ) {

        if( ! (entity instanceof String) ) {
           throw new IllegalArgumentException("Only String's are accepted when sending SVG images");
        }
        return (String) entity;
    }

}
