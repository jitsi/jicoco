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

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.jetbrains.annotations.*;
import org.jitsi.health.*;

/**
 * A generic health check REST endpoint which checks the health using a
 * a {@link HealthCheckService}, if one is present.
 *
 */
@Path("/about/health")
public class Health
{
    @NotNull
    private final HealthCheckService healthCheckService;

    public Health(@NotNull HealthCheckService healthCheckService)
    {
        this.healthCheckService = healthCheckService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHealth()
    {
        Result result = healthCheckService.getResult();
        if (!result.getSuccess())
        {
            int status
                = result.getResponseCode() != null ? result.getResponseCode() : result.getHardFailure() ? 500 : 503;
            Response.ResponseBuilder response = Response.status(status);
            if (result.getMessage() != null)
            {
                response.entity(result.getMessage());
            }

            return response.build();
        }

        return Response.ok().build();
    }
}
