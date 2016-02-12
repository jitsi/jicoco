/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.influxdb;

import net.java.sip.communicator.util.*;

import org.jitsi.eventadmin.*;
import org.jitsi.osgi.*;
import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

import java.util.*;

/**
 * Implements a <tt>BundleActivator</tt> for <tt>LoggingService</tt>.
 *
 * @author Boris Grozev
 * @author George Politis
 * @author Pawel Domas
 */
public abstract class AbstractActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>Activator</tt> class
     * and its instances to print debug information.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractLoggingHandler.class);

    private ServiceRegistration<EventHandler> serviceRegistration;

    /**
     * Initializes a <tt>LoggingService</tt>.
     *
     * @param bundleContext the <tt>bundleContext</tt> to use.
     * @throws Exception
     */
    @Override
    public void start(BundleContext bundleContext)
        throws Exception
    {
        ConfigurationService cfg =
            ServiceUtils2.getService(bundleContext, ConfigurationService.class);

        if (cfg.getBoolean(AbstractLoggingHandler.ENABLED_PNAME, false))
        {
            AbstractLoggingHandler handler;

            try
            {
                handler = createHandler(cfg);
            }
            catch (Exception e)
            {
                logger.warn("Failed to instantiate LoggingHandler: " + e);
                return;
            }

            String[] topics = { "org/jitsi/*" };

            serviceRegistration
                = EventUtil.registerEventHandler(
                    bundleContext, topics, handler);
        }
    }

    /**
     * Creates and returns an implementation of <tt>LoggingHandler</tt>.
     */
    protected abstract AbstractLoggingHandler
        createHandler(ConfigurationService cfg)
            throws Exception;

    /**
     * Removes the previously initialized <tt>LoggingService</tt> instance from
     * <tt>bundleContext</tt>.
     *
     * @param bundleContext the <tt>bundleContext</tt> to use.
     * @throws Exception
     */
    @Override
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        if (serviceRegistration != null)
            serviceRegistration.unregister();
    }
}
