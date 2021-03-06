/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openspaces.example.data.feeder;

import org.openspaces.core.SpaceInterruptedException;
import org.openspaces.example.data.common.Data;
import org.openspaces.example.data.common.IDataProcessor;
import org.openspaces.remoting.EventDrivenProxy;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A bean that uses OpenSpaces Remoting support to invoke IDataProcessor implementation
 * exposed by another processing unit.
 *
 * <p>Starts up a scheduled task that invokes both IDataProcessor APIs periodically. Uses
 * java.util.concurrent Scheduled Executor Service.
 *
 * <p>Note, this bean simply uses IDataProcessor, OpenSpaces Remoting hides the fact that
 * this interface will actually cause a remote invocation, with the Space as the transport
 * layer, directed into a service exposed by another processing unit.
 *
 * @author kimchy
 */
public class DataRemoting {

    private long numberOfTypes = 10;

    private long defaultDelay = 1000;

    @EventDrivenProxy(gigaSpace = "gigaSpace", timeout = 30000)
    private IDataProcessor dataProcessor;

    private ScheduledExecutorService executorService;

    private ScheduledFuture<?> sf;

    /**
     * Sets the number of types that will be used to set {@link org.openspaces.example.data.common.Data#setType(Long)}.
     *
     * <p>The type is used as the routing index for partitioned space. This will affect the distribution of Data
     * objects over a partitioned space.
     */
    public void setNumberOfTypes(long numberOfTypes) {
        this.numberOfTypes = numberOfTypes;
    }

    public void setDefaultDelay(long defaultDelay) {
        this.defaultDelay = defaultDelay;
    }

    @PostConstruct
    public void construct() {
        System.out.println("--- STARTING REMOTING WITH CYCLE [" + defaultDelay + "]");
        executorService = Executors.newScheduledThreadPool(1);
        sf = executorService.scheduleAtFixedRate(new DataFeederTask(), 0, defaultDelay, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void destroy() {
        sf.cancel(false);
        sf = null;
        executorService.shutdown();
    }

    public class DataFeederTask implements Runnable {

        private int counter;

        public void run() {
            try {
                long time = System.currentTimeMillis();
                Data data = new Data((counter++ % numberOfTypes), "FEEDER " + Long.toString(time));
                data.setProcessed(false);
                System.out.println("--- REMOTING PARAMETER " + data);
                dataProcessor.sayData(data);
                data = dataProcessor.processData(data);
                System.out.println("--- REMOTING RESULT   " + data);
            } catch (SpaceInterruptedException e) {
                // ignore, we are being shutdown
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}