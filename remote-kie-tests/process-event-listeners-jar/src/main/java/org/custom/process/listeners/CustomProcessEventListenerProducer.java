package org.custom.process.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jbpm.runtime.manager.api.qualifiers.Process;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.internal.runtime.manager.EventListenerProducer;

/**
 * See http://docs.jboss.org/jbpm/v6.2/userguide/jBPMCoreEngine.html#d0e2270
 */
@Process
public class CustomProcessEventListenerProducer implements EventListenerProducer<CustomProcessEventListener> {

    @Override
    public List<CustomProcessEventListener> getEventListeners( String identifier, Map<String, Object> params ) {
        System.out.println( "Getting event listeners: " + identifier);
        for( String key : params.keySet() ) { 
            System.out.println( "Key: " + key);
        }
        RuntimeManager runtimeManager = (RuntimeManager) params.get("runtimeManager");
        CustomProcessEventListener[] listenerArr = { new CustomProcessEventListener(runtimeManager) };
        // create a new ArrayList because Arrays.asList() returns an unmodifiable list
        // (and I'm not sure if the returned list will be modified somewhere?)
        return new ArrayList<CustomProcessEventListener>(Arrays.asList(listenerArr));
    }

}
