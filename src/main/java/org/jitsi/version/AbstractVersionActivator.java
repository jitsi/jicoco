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
package org.jitsi.version;

import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.version.Version;
import org.jitsi.service.version.*;

import org.osgi.framework.*;

import java.util.regex.*;

/**
 * <p>
 * The entry point to the {@code VersionService} implementation. We register the
 * {@code VersionServiceImpl} instance on the OSGi bus.
 * </p>
 * <p>
 * This abstract <tt>BundleActivator</tt> will provide implementation of
 * the <tt>VersionService</tt> once {@link #getCurrentVersion()} method is
 * provided.
 * </p>
 *
 * @author George Politis
 * @author Pawel Domas
 */
public abstract class AbstractVersionActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by this <tt>VersionActivator</tt> instance for
     * logging output.
     */
    private final Logger logger
        = Logger.getLogger(AbstractVersionActivator.class);

    /**
     * The pattern that will parse strings to version object.
     */
    private static final Pattern PARSE_VERSION_STRING_PATTERN
        = Pattern.compile("(\\d+)\\.(\\d+)\\.([\\d\\.]+)");

    /**
     * The OSGi <tt>BundleContext</tt>.
     */
    private static BundleContext bundleContext;

    /**
     * <tt>VersionImpl</tt> instance which stores the current version of the
     * application.
     */
    private VersionImpl currentVersion;

    /**
     * Implementing class must return a valid {@link CurrentVersion} object.
     *
     * @return {@link CurrentVersion} instance which provides the details about
     * current version of the application.
     */
    abstract protected CurrentVersion getCurrentVersion();

    /**
     * Called when this bundle is started so the Framework can perform the
     * bundle-specific activities necessary to start this bundle.
     *
     * @param context The execution context of the bundle being started.
     * @throws Exception If this method throws an exception, this bundle is
     * marked as stopped and the Framework will remove this bundle's listeners,
     * unregister all services registered by this bundle, and release all
     * services used by this bundle.
     */
    public void start(BundleContext context) throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("Started.");

        AbstractVersionActivator.bundleContext = context;

        CurrentVersion currentVersion = getCurrentVersion();

        this.currentVersion = new VersionImpl(
                currentVersion.getDefaultAppName(),
                currentVersion.getMajorVersion(),
                currentVersion.getMinorVersion(),
                currentVersion.getNightlyBuildID(),
                currentVersion.getPreReleaseID());

        VersionServiceImpl versionServiceImpl = new VersionServiceImpl();

        context.registerService(
                VersionService.class.getName(),
                versionServiceImpl,
                null);

        String applicationName = this.currentVersion.getApplicationName();
        String versionString = this.currentVersion.toString();

        logger.debug(
                this.currentVersion.getApplicationName()
                    + " Version Service ... [REGISTERED]");

        if (logger.isInfoEnabled())
        {
            logger.info(
                    applicationName + " Version: " + applicationName + " "
                        + versionString);
        }

        //register properties for those that would like to use them
        ConfigurationService cfg = getConfigurationService();

        cfg.setProperty(Version.PNAME_APPLICATION_NAME, applicationName, true);
        cfg.setProperty(Version.PNAME_APPLICATION_VERSION, versionString, true);
    }

    /**
     * Gets a <tt>ConfigurationService</tt> implementation currently registered
     * in the <tt>BundleContext</tt> in which this bundle has been started or
     * <tt>null</tt> if no such implementation was found.
     *
     * @return a <tt>ConfigurationService</tt> implementation currently
     * registered in the <tt>BundleContext</tt> in which this bundle has been
     * started or <tt>null</tt> if no such implementation was found
     */
    private static ConfigurationService getConfigurationService()
    {
        return
            ServiceUtils.getService(bundleContext, ConfigurationService.class);
    }

    /**
     * Gets the <tt>BundleContext</tt> instance within which this bundle has
     * been started.
     *
     * @return the <tt>BundleContext</tt> instance within which this bundle has
     * been started
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is still
     * marked as stopped, and the Framework will remove the bundle's listeners,
     * unregister all services registered by the bundle, and release all
     * services used by the bundle.
     */
    public void stop(BundleContext context) throws Exception
    {
    }

    /**
     * Implementation of the {@link VersionService}.
     */
    class VersionServiceImpl
        implements VersionService
    {
        /**
         * Returns a Version instance corresponding to the <tt>version</tt>
         * string.
         *
         * @param version a version String that we have obtained by calling a
         * <tt>Version.toString()</tt> method.
         *
         * @return the <tt>Version</tt> object corresponding to the
         * <tt>version</tt> string. Or null if we cannot parse the string.
         */
        public Version parseVersionString(String version)
        {
            Matcher matcher = PARSE_VERSION_STRING_PATTERN.matcher(version);

            if(matcher.matches() && matcher.groupCount() == 3)
            {
                return new VersionImpl(
                    currentVersion.getApplicationName(),
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    matcher.group(3),
                    currentVersion.getPreReleaseID());
            }

            return null;
        }

        /**
         * Returns a <tt>Version</tt> object containing version details of the
         * the application version that we're currently running.
         *
         * @return a <tt>Version</tt> object containing version details of the
         * application version that we're currently running.
         */
        public Version getCurrentVersion()
        {
            return currentVersion;
        }
    }
}
