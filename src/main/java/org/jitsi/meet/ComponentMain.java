/*
 * Copyright @ 2018 - present 8x8, Inc.
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

import org.jitsi.osgi.*;
import org.jitsi.retry.*;
import org.jitsi.service.configuration.*;
import org.jitsi.utils.*;
import org.jitsi.utils.logging.*;
import org.jitsi.xmpp.component.*;

import org.jivesoftware.whack.*;

import org.osgi.framework.*;

import org.xeustechnologies.jcl.*;
import org.xmpp.component.*;

import java.util.concurrent.*;

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
     * Retry strategy for "first time connect". After we successfully add
     * external component to <tt>ExternalComponentManager</tt>(it is connected)
     * those will be handled by <tt>ExternalComponentManager</tt> internally.
     */
    private RetryStrategy connectRetry;

    /**
     * Lock used to synchronize first time connect attempt with cancel job.
     */
    private final Object connectSynRoot = new Object();

    /**
     * Executor service used to execute connect retry attempts.
     */
    private ScheduledExecutorService executorService;

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
        ConfigUtils.PASSWORD_SYS_PROPS = "pass";
        ConfigUtils.PASSWORD_CMD_LINE_ARGS = "secret,user_password";

        OSGi.setBundleConfig(bundleConfig);

        bundleConfig.setSystemPropertyDefaults();

        ClassLoader classLoader = loadBundlesJars(bundleConfig);
        OSGi.setClassLoader(classLoader);

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
                    ServiceUtils2.getService(
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

        // Start OSGi
        OSGi.start(activator);

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
     * Creates class loader that able to load classes from jars of selected by
     * bundleConfig {@link OSGiBundleConfig#BUNDLES_JARS_PATH} parameter.
     * @param bundleConfig - instance with path to extended bundles jar.
     * @return OSGi class loader for bundles.
     */
    private ClassLoader loadBundlesJars(OSGiBundleConfig bundleConfig) {
        String bundlesJarsPath = bundleConfig.getBundlesJarsPath();
        if (bundlesJarsPath == null)
        {
            return ClassLoader.getSystemClassLoader();
        }

        JarClassLoader jcl = new JarClassLoader();
        jcl.add(bundlesJarsPath + "/");
        return new OSGiClassLoader(jcl, ClassLoader.getSystemClassLoader());
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

        this.componentManager = new ExternalComponentManager(host, port, false);

        String componentSubDomain = component.getSubdomain();
        componentManager.setSecretKey(
            componentSubDomain, component.getSecret());

        componentManager.setServerName(component.getDomain());

        this.executorService = Executors.newScheduledThreadPool(1);

        component.init();

        connectRetry = new RetryStrategy(executorService);

        connectRetry.runRetryingTask(
            new SimpleRetryTask(0, 5000, true, getConnectCallable()));

        return true;
    }

    /**
     * Disconnects XMPP component if one is currently in use.
     */
    private void stopComponent()
    {
        synchronized (connectSynRoot)
        {
            if (component == null || componentManager == null)
                return;

            if (connectRetry != null)
            {
                connectRetry.cancel();
                connectRetry = null;
            }

            if (executorService != null)
                executorService.shutdown();

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

            component = null;
            componentManager = null;
        }
    }

    /**
     * The callable returned by this method describes the task of connecting
     * XMPP component.
     *
     * @return <tt>Callable<Boolean></tt> which returns <tt>true</tt> as long
     *         as we're failing to connect.
     */
    private Callable<Boolean> getConnectCallable()
    {
        return () ->
        {
            try
            {
                synchronized (connectSynRoot)
                {
                    if (componentManager == null || component == null)
                    {
                        // Task cancelled ?
                        return false;
                    }

                    componentManager.addComponent(
                        component.getSubdomain(), component);

                    return false;
                }
            }
            catch (ComponentException e)
            {
                logger.error(
                    e.getMessage() +
                        ", host:" + component.getHostname() +
                        ", port:" + component.getPort(), e);
                return true;
            }
        };
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
