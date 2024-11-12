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

import com.typesafe.config.ConfigObject
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.jitsi.utils.logging2.createLogger
import java.io.FileReader
import java.security.PrivateKey
import java.time.Duration

data class JwtInfo(
    val privateKey: PrivateKey,
    val kid: String,
    val issuer: String,
    val audience: String,
    val ttl: Duration
) {
    companion object {
        private val logger = createLogger()
        fun fromConfig(jwtConfigObj: ConfigObject): JwtInfo {
            // Any missing or incorrect value here will throw, which is what we want:
            // If anything is wrong, we should fail to create the JwtInfo
            val jwtConfig = jwtConfigObj.toConfig()
            logger.info("got jwtConfig: ${jwtConfig.root().render()}")
            try {
                return JwtInfo(
                    privateKey = parseKeyFile(jwtConfig.getString("signing-key-path")),
                    kid = jwtConfig.getString("kid"),
                    issuer = jwtConfig.getString("issuer"),
                    audience = jwtConfig.getString("audience"),
                    ttl = jwtConfig.getDuration("ttl").withMinimum(Duration.ofMinutes(10))
                )
            } catch (t: Throwable) {
                logger.info("Unable to create JwtInfo: $t")
                throw t
            }
        }
    }
}

private fun parseKeyFile(keyFilePath: String): PrivateKey {
    val parser = PEMParser(FileReader(keyFilePath))
    return (parser.readObject() as PEMKeyPair).let { pemKeyPair ->
        JcaPEMKeyConverter().getKeyPair(pemKeyPair).private
    }
}

/**
 * Returns [min] if this Duration is less than that minimum, otherwise this
 */
private fun Duration.withMinimum(min: Duration): Duration = maxOf(this, min)
