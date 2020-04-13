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

import org.eclipse.jetty.http.*;
import org.glassfish.hk2.utilities.binding.*;
import org.glassfish.jersey.server.*;
import org.glassfish.jersey.test.*;
import org.jitsi.health.*;
import org.junit.*;

import javax.ws.rs.core.*;

import static junit.framework.TestCase.*;
import static org.mockito.Mockito.*;

public class HealthTest extends JerseyTest
{
    protected HealthCheckServiceProvider healthCheckServiceProvider;
    protected HealthCheckService healthCheckService;
    protected static final String BASE_URL = "/about/health";

    @Override
    protected Application configure()
    {
        healthCheckServiceProvider = mock(HealthCheckServiceProvider.class);
        healthCheckService = mock(HealthCheckService.class);
        when(healthCheckServiceProvider.get()).thenReturn(healthCheckService);

        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        return new ResourceConfig() {
            {
                register(new AbstractBinder()
                {
                    @Override
                    protected void configure()
                    {
                        bind(healthCheckServiceProvider).to(HealthCheckServiceProvider.class);
                    }
                });
                register(Health.class);
            }
        };
    }

    @Test
    public void testSuccessfulHealthCheck()
    {
        when(healthCheckService.getResult()).thenReturn(null);

        Response resp = target(BASE_URL).request().get();
        assertEquals(HttpStatus.OK_200, resp.getStatus());
    }

    @Test
    public void testFailingHealthCheck()
    {
        when(healthCheckService.getResult()).thenThrow(new RuntimeException("Health check failed"));
        Response resp = target(BASE_URL).request().get();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, resp.getStatus());
    }
}