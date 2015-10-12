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

import org.jitsi.impl.osgi.framework.*;
import org.osgi.framework.*;

/**
 * Represents the entry point of the OSGi environment of the Jitsi Meet
 * component applications(JVB, Jicofo, Jigasi etc.).
 *
 * @author Pawel Domas
 */
public class OSGi
{
    /**
     * The <tt>OSGiLauncher</tt> instance which
     * represents the launched OSGi instance.
     */
    private static OSGiLauncher launcher;

    /**
     * <tt>BundleActivator</tt> bundle activator launched on startup/shutdown of
     * the OSGi system.
     */
    private static BundleActivator activator;

    /**
     * OSGi bundles descriptor
     */
    private static OSGiBundleConfig bundleConfig;

    /**
     * Starts the OSGi infrastructure.
     *
     * @param activator the <tt>BundleActivator</tt> that will be launched after
     *        OSGi starts. {@link BundleActivator#stop(BundleContext)} will be
     *        called on OSGi shutdown.
     */
    public static synchronized void start(BundleActivator activator)
    {
        if (OSGi.activator != null)
            throw new IllegalStateException("activator");

        if (OSGi.bundleConfig == null)
            throw new IllegalStateException("Bundle config not initialized");

        OSGi.activator = activator;

        if (launcher == null)
        {
            String[][] bundles = bundleConfig.getBundles();

            launcher = new OSGiLauncher(bundles);
        }

        launcher.start(activator);
    }

    /**
     * Stops the OSGi system.
     *
     * The <tt>BundleActivator</tt> that has been passed to
     * {@link #start(BundleActivator)} will be launched after shutdown.
     */
    public static synchronized void stop()
    {
        if (launcher != null && activator != null)
        {
            launcher.stop(activator);

            activator = null;
        }
    }

    /**
     * Returns the instance of {@link OSGiBundleConfig} currently in use by the
     * system.
     */
    public static OSGiBundleConfig getBundleConfig()
    {
        return bundleConfig;
    }

    /**
     * Modifies OSGi bundles config.
     * @param bundleConfig the OSGi config that describes OSGi bundles to be
     *                     used
     * @throws IllegalStateException on config change attempt while the OSGi
     *         system is running.
     */
    public static void setBundleConfig(OSGiBundleConfig bundleConfig)
    {
        if (activator != null)
        {
            throw new IllegalStateException(
                "Can not change OSGi config while the system is running");
        }
        OSGi.bundleConfig = bundleConfig;
    }
}
