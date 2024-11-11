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
package org.jitsi.jwt

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.time.Clock
import java.time.Duration
import java.util.*

class RefreshingJwt(
    private val jwtInfo: JwtInfo?,
    private val clock: Clock = Clock.systemUTC()
) : RefreshingProperty<String?>(
    // We refresh 5 minutes before the expiration
    jwtInfo?.ttl?.minus(Duration.ofMinutes(5)) ?: Duration.ofSeconds(Long.MAX_VALUE),
    clock,
    {
        jwtInfo?.let {
            Jwts.builder()
                .setHeaderParam("kid", it.kid)
                .setIssuer(it.issuer)
                .setAudience(it.audience)
                .setExpiration(Date.from(clock.instant().plus(it.ttl)))
                .signWith(it.privateKey, SignatureAlgorithm.RS256)
                .compact()
        }
    }
)
