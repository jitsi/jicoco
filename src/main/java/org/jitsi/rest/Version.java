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

package org.jitsi.rest;

import com.fasterxml.jackson.annotation.*;
import org.jitsi.osgi.*;
import org.jitsi.utils.version.*;

import javax.inject.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

/**
 * A generic version REST endpoint which pulls version information from
 * a {@link VersionService}, if one is present.  Otherwise returns
 * {@link Version#UNKNOWN_VERSION}
 *
 */
@Path("/about/version")
public class Version
{
    @Inject
    protected VersionServiceProvider versionServiceProvider;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public VersionInfo getVersion()
    {
        VersionService versionService = versionServiceProvider.get();
        if (versionService == null)
        {
            return UNKNOWN_VERSION;
        }
        org.jitsi.utils.version.Version version = versionService.getCurrentVersion();

        return new VersionInfo(
            version.getApplicationName(),
            version.toString(),
            System.getProperty("os.name")
        );
    }

    static class VersionInfo {
        @JsonProperty String name;
        @JsonProperty String version;
        @JsonProperty String os;

        public VersionInfo() {}
        public VersionInfo(String name, String version, String os)
        {
            this.name = name;
            this.version = version;
            this.os = os;
        }
    }

    protected static VersionInfo UNKNOWN_VERSION = new VersionInfo("", "", "");
}
