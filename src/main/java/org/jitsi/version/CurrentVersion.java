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

/**
 * The interface provides the details about the current version of
 * the application.
 *
 * @author Pawel Domas
 */
public interface CurrentVersion
{
    /**
     * Method must return default name of the application.
     *
     * @return a <tt>String</tt> which is the default value for the application
     * name.
     */
    String getDefaultAppName();

    /**
     * Returns the version major of the current application version. In an
     * example 2.3.1 version string 2 is the version major. The version major
     * number changes when a relatively extensive set of new features and
     * possibly rearchitecturing have been applied to the application.
     *
     * @return the version major String.
     */
    int getMajorVersion();

    /**
     * Returns the version minor of the current application version. In an
     * example 2.3.1 version string 3 is the version minor. The version minor
     * number changes after adding enhancements and possibly new features to a
     * given application version.
     *
     * @return the version minor integer.
     */
    int getMinorVersion();

    /**
     * If this is a nightly build, returns the build identifies (e.g.
     * nightly-2007.12.07-06.45.17). If this is not a nightly build version,
     * the method returns <tt>null</tt>.
     *
     * @return a String containing a nightly build identifier or <tt>null</tt>
     * if this is a release version and therefore not a nightly build.
     */
    String getNightlyBuildID();

    /**
     * Returns the version pre-release ID of the current application version
     * and <tt>null</tt> if this version is not a pre-release. Version
     * pre-release id-s exist only for pre-release versions and are
     * <tt>null<tt/> otherwise.
     *
     * @return a String containing the version pre-release ID.
     */
    String getPreReleaseID();
}
