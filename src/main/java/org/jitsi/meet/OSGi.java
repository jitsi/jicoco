/*
 * Copyright @ 2015 - present, 8x8 Inc
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
     * OSGi bundles descriptor
     */
    private static OSGiBundleConfig bundleConfig;

    /**
     * OSGi class loader
     */
    private static ClassLoader classLoader;

    /**
     * Starts the OSGi infrastructure.
     *
     * @param activator the <tt>BundleActivator</tt> that will be launched after
     *        OSGi starts.
     */
    public static synchronized void start(BundleActivator activator)
    {
        if (activator == null)
            throw new NullPointerException("activator");

        if (OSGi.bundleConfig == null)
            throw new IllegalStateException("Bundle config not initialized");

        if (classLoader == null)
            throw new IllegalStateException("Class Loader not initialized");

        if (launcher == null)
        {
            String[][] bundles = bundleConfig.getBundles();
            launcher = new OSGiLauncher(bundles, classLoader);
        }

        launcher.start(activator);
    }

    /**
     * Stops the OSGi system.
     *
     * @param activator the <tt>BundleActivator</tt> which
     *        {@link BundleActivator#stop(BundleContext)} method will be called
     *        after OSGi shutdown.
     */
    public static synchronized void stop(BundleActivator activator)
    {
        if (activator == null)
            throw new NullPointerException("activator");

        if (launcher != null)
        {
            launcher.stop(activator);
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
     */
    public static void setBundleConfig(OSGiBundleConfig bundleConfig)
    {
        OSGi.bundleConfig = bundleConfig;
    }

    /**
     * Modifies OSGi class loader.
     * @param classLoader this class loader would be used to load and
     *                    instantiate bundles.
     */
    public static void setClassLoader(ClassLoader classLoader) {
        OSGi.classLoader = classLoader;
    }

}
