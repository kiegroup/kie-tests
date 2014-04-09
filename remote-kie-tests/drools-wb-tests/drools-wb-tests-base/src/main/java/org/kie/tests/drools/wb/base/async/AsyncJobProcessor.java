/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.kie.tests.drools.wb.base.async;

import static javax.ejb.LockType.READ;

import java.util.concurrent.Future;

import javax.ejb.AccessTimeout;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.Singleton;

import org.kie.tests.drools.wb.base.async.ThreadManagerResource.JobCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AsyncJobProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AsyncJobProcessor.class);
    
    @Asynchronous
    @Lock(READ)
    @AccessTimeout(-1)
    public Future<String> submitJob(JobCallable job) throws Exception {
        logger.info( ">");
        try {
            String result = job.call();
            return new AsyncResult<String>(result);
        } catch( Exception e ) { 
            logger.warn( e.getClass().getSimpleName() + ": " + e.getMessage());
            throw e;
        } finally { 
            logger.info( "<");
        }
    }

}
