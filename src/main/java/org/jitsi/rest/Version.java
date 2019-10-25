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
