package org.kie.tests.drools.wb.base.async;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Path("/thread")
@ManagedBean
public class ThreadManagerResource {

    private static final Logger logger = LoggerFactory.getLogger(ThreadManagerResource.class);
  
    @Inject
    private AsyncJobProcessor jobProcessor;
  
    private final static AtomicInteger idGen = new AtomicInteger(1);
    private Map<Integer, Future<String>> cache = new HashMap<Integer, Future<String>>();
   
    private boolean useThreadPool = false;
    private int threadPoolSize = 4;
    
    private ExecutorService executorService;
  
    public ThreadManagerResource() { 
      try { 
        Context initCtx = new InitialContext();
        Context envCtx = (Context) initCtx.lookup("java:comp/env");

        // Look up our data source
        String useThreadPoolProp = (String)envCtx.lookup("org.drools.wb.async.thread");
        if( useThreadPoolProp != null ) { 
            useThreadPool = Boolean.parseBoolean(useThreadPoolProp);
        }
        String threadPoolSizeProp = (String)envCtx.lookup("org.drools.wb.async.thread.size");
        if( threadPoolSizeProp != null ) { 
            threadPoolSize = Integer.parseInt(threadPoolSizeProp);
        }
      } catch( Exception e) { 
          // do nothing
      }
      
       if( useThreadPool ) { 
           executorService = Executors.newFixedThreadPool(threadPoolSize);
       }
    }
    private Future<String> submitJob(JobCallable job) throws Exception { 
       logger.info( "] THREAD: " + useThreadPool );
       if( useThreadPool ) { 
           return executorService.submit(job);
       } else { 
          return jobProcessor.submitJob(job);
       }
    }
    
    @PostConstruct
    public void init() { 
        logger.info( "] INIT" );
    }
    
    @GET
    @Path("/ping")
    @Produces(MediaType.APPLICATION_JSON)
    public Response ping() throws Exception { 
        logger.info( "] PING!");
        Future<String> result = submitJob(new JobCallable());
        int id = idGen.getAndIncrement();
        cache.put(id, result);
        logger.info( "[ " + id + "/RUNNING" );
        return createResponse(new JaxbPingResponse(id, "RUNNING"));
    }
    
    @GET
    @Path("/pung/{id: [0-9]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pung(@PathParam("id") Integer id) {
        logger.info( "] PUNG!");
        String result = "NULL";
        Future<String> resultFuture = cache.get(id);
        if( resultFuture != null ) { 
            result = "RUNNING";
            try { 
                result = resultFuture.get(1, TimeUnit.MILLISECONDS);
            } catch( Exception e ){ 
                // do nothing
            }
        }
        logger.info( "[ " + result);
        return createResponse(new JaxbPingResponse(id, result));
    }
   
    private Response createResponse(JaxbPingResponse pingResponse) { 
        ResponseBuilder responseBuilder = Response.ok(pingResponse);
        return responseBuilder.build();
    }
    
    public static class JobCallable implements Callable<String> {
        @Override
        public String call() throws Exception {
            System.out.println( "SLEEP" );
            Thread.currentThread().sleep(2*1000);
            System.out.println( "DONE" );
            return "DONE";
        }
    }
}
