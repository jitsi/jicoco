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

import static org.junit.jupiter.api.Assertions.*;

import jakarta.ws.rs.core.*;
import org.eclipse.jetty.http.*;
import org.glassfish.jersey.server.*;
import org.glassfish.jersey.test.*;
import org.jitsi.utils.version.*;
import org.junit.jupiter.api.*;

public class VersionTest
    extends JerseyTest
{
    protected static final String BASE_URL = "/about/version";

    @Override
    protected Application configure()
    {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        return new ResourceConfig()
        {
            {
                register(new Version(new VersionImpl("appName", 2, 0)));
            }
        };
    }

    @Test
    public void testVersion()
    {
        Response resp = target(BASE_URL).request().get();
        assertEquals(HttpStatus.OK_200, resp.getStatus());
        Version.VersionInfo versionInfo =
            resp.readEntity(Version.VersionInfo.class);
        assertEquals("appName", versionInfo.name);
        assertEquals("2.0", versionInfo.version);
    }
}
