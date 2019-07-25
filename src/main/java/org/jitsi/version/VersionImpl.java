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
package org.jitsi.version;

import org.jitsi.utils.version.*;
import org.jitsi.utils.*;

/**
 * Implements {@link Version} interface by providing the pieces missing from
 * {@link AbstractVersion}.
 *
 * @author Pawel Domas
 */
class VersionImpl
    extends AbstractVersion
{
    /**
     * The application name.
     */
    private String applicationName;

    /**
     * The default application name which will be used in case we fail to obtain
     * one in {@link #getApplicationName()}.
     */
    private final String defaultApplicationName;

    /**
     * The nightly build ID.
     */
    private final String nightlyBuildId;

    /**
     * Pre-release ID holder.
     */
    private final String preReleaseId;

    /**
     * Creates version object with custom major, minor and nightly build id.
     *
     * @param defaultApplicationName default application name which will be used
     * if we fail to obtain one from the <tt>ResourceManagementService</tt>.
     * @param majorVersion the major version to use.
     * @param minorVersion the minor version to use.
     * @param nightlyBuildID the nightly build id value for new version object.
     */
    VersionImpl(String defaultApplicationName,
                int majorVersion,
                int minorVersion,
                String nightlyBuildID,
                String preReleaseId)
    {
        super(majorVersion, minorVersion, nightlyBuildID);

        this.defaultApplicationName = defaultApplicationName;
        this.nightlyBuildId = nightlyBuildID;
        this.preReleaseId = preReleaseId;
    }

    /**
     * Indicates if this application version corresponds to a nightly
     * build of a repository snapshot or to an official release.
     *
     * @return {@code true} if this is a build of a nightly repository snapshot
     * and {@code false} if this is an official release.
     */
    public boolean isNightly()
    {
        return !StringUtils.isNullOrEmpty(nightlyBuildId);
    }

    /**
     * Indicates whether this version represents a prerelease (i.e. an
     * incomplete release like an alpha, beta or release candidate version).
     *
     * @return {@code true} if this version represents a prerelease and
     * {@code false} otherwise.
     */
    public boolean isPreRelease()
    {
        return !StringUtils.isNullOrEmpty(preReleaseId);
    }

    /**
     * Returns the version prerelease ID of the current application
     * version and {@code null} if this version is not a prerelease.
     *
     * @return a {@code String} containing the version prerelease ID.
     */
    public String getPreReleaseID()
    {
        return isPreRelease() ? preReleaseId : null;
    }

    /**
     * Returns the name of the application that we're currently running.
     *
     * @return the name of the application that we're currently running.
     */
    public String getApplicationName()
    {
        if (applicationName == null)
        {
            applicationName = defaultApplicationName;
        }
        return applicationName;
    }
}
