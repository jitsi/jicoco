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
package org.jitsi.meet;

import net.java.sip.communicator.service.shutdown.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;
import org.jitsi.impl.configuration.*;
import org.jitsi.service.configuration.*;
import org.jitsi.xmpp.component.*;
import org.jivesoftware.whack.*;
import org.osgi.framework.*;
import org.xmpp.component.*;

/**
 * Class for running main program loop of Jitsi Meet components like
 * Jitsi-videobridge, Jicofo, Jigasi etc.
 *
 * @author Pawel Domas
 */
public class ComponentMain
{
    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(ComponentMain.class);

    /**
     * The <tt>Object</tt> which synchronizes the access to the state related to
     * the decision whether the application is to exit. {@link
     * Runtime#addShutdownHook(Thread)} is used to detect component shutdown.
     */
    private static final Object exitSyncRoot = new Object();

    /**
     * Class which implements XMPP component. It can be <tt>null</tt> if we're
     * not running XMPP interface in current session, but REST only(component
     * impl dependent).
     */
    private ComponentBase component;

    /**
     * External component manager used to connect the component to Prosody.
     */
    private ExternalComponentManager componentManager;

    /**
     * Runs "main" program loop until it gets killed or stopped by shutdown hook
     * @param bundleConfig OSGi bundles configuration that describes the system.
     */
    public void runMainProgramLoop(OSGiBundleConfig bundleConfig)
    {
        runMainProgramLoop(null, bundleConfig);
    }

    /**
     * Runs "main" program loop until it gets killed or stopped by shutdown hook
     * @param component XMPP component instance to run in this session(optional)
     * @param bundleConfig OSGi bundles configuration that describes the system
     */
    public void runMainProgramLoop(ComponentBase component,
                                   OSGiBundleConfig bundleConfig)
    {
        // Make sure that passwords are not printed by ConfigurationService
        // on startup by setting password regExpr and cmd line args list
        ConfigurationServiceImpl.PASSWORD_SYS_PROPS = "pass";
        ConfigurationServiceImpl
            .PASSWORD_CMD_LINE_ARGS = "secret,user_password";

        OSGi.setBundleConfig(bundleConfig);

        bundleConfig.setSystemPropertyDefaults();

        /*
         * Start OSGi. It will invoke the application programming interfaces
         * (APIs) of Jitsi Videobridge. Each of them will keep the application
         * alive.
         */
        BundleActivator activator = new BundleActivator()
            {
                @Override
                public void start(BundleContext bundleContext)
                    throws Exception
                {
                    registerShutdownService(bundleContext);

                    // Log config properties(hide password values)
                    ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class)
                        .logConfigurationProperties("(pass)|(secret)");
                }

                @Override
                public void stop(BundleContext bundleContext)
                    throws Exception
                {
                    // We're doing nothing
                }
            };

        // Register shutdown hook to perform cleanup before exit
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            public void run()
            {
                synchronized (exitSyncRoot)
                {
                    exitSyncRoot.notifyAll();
                }
            }
        });

        // FIXME: implement startComponent retries in case Prosody is not
        // available yet(currently we'll stop on the exception thrown by
        // startComponent)
        if (component == null || startComponent(component))
        {
            try
            {
                synchronized (exitSyncRoot)
                {
                    exitSyncRoot.wait();
                }
            }
            catch (Exception e)
            {
                logger.error(e, e);
            }
        }

        stopComponent();

        OSGi.stop(activator);
    }

    /**
     * Starts XMPP component.
     * @param component the instance of XMPP component to start.
     * @return <tt>true</tt> if XMPP component has been started successfully.
     */
    private boolean startComponent(ComponentBase component)
    {
        this.component = component;

        String host = component.getHostname();
        int port = component.getPort();

        this.componentManager = new ExternalComponentManager(host, port);

        String componentSubDomain = component.getSubdomain();
        componentManager.setSecretKey(
            componentSubDomain, component.getSecret());

        componentManager.setServerName(component.getDomain());

        try
        {
            componentManager.addComponent(
                component.getSubdomain(), component);
        }
        catch (ComponentException e)
        {
            logger.error(
                e.getMessage() + ", host:" + host + ", port:" + port, e);

            return false;
        }

        component.init();

        return true;
    }

    /**
     * Disconnects XMPP component if one is currently in use.
     */
    private void stopComponent()
    {
        if (component == null || componentManager == null)
            return;

        component.shutdown();
        try
        {
            componentManager.removeComponent(component.getSubdomain());
        }
        catch (ComponentException e)
        {
            logger.error(e, e);
        }

        component.dispose();
    }

    /**
     * Registers {@link ShutdownService} implementation for videobridge
     * application.
     * @param bundleContext the OSGi context
     */
    private static void registerShutdownService(BundleContext bundleContext)
    {
        bundleContext.registerService(
            ShutdownService.class,
            new ShutdownService()
            {
                private boolean shutdownStarted = false;

                @Override
                public void beginShutdown()
                {
                    if (shutdownStarted)
                        return;

                    shutdownStarted = true;

                    synchronized (exitSyncRoot)
                    {
                        exitSyncRoot.notifyAll();
                    }
                }
            }, null
        );
    }
}
