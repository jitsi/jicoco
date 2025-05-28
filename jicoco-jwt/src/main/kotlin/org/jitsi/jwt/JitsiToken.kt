/*
 * Copyright @ 2025 - present 8x8, Inc.
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class JitsiToken(
    val iss: String? = null,
    val aud: String? = null,
    val iat: Long? = null,
    val nbf: Long? = null,
    val exp: Long? = null,
    val name: String? = null,
    val picture: String? = null,
    @JsonProperty("user_id")
    val userId: String? = null,
    val email: String? = null,
    @JsonProperty("email_verified")
    val emailVerified: Boolean? = null,
    val room: String? = null,
    val sub: String? = null,
    val context: Context? = null
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Context(
        val user: User?,
        val group: String? = null,
        val tenant: String? = null,
        val features: Features? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class User(
        val id: String? = null,
        val name: String? = null,
        val avatar: String? = null,
        val email: String? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Features(
        val flip: Boolean? = null,
        val livestreaming: Boolean? = null,
        @JsonProperty("outbound-call")
        val outboundCall: Boolean? = null,
        val recording: Boolean? = null,
        @JsonProperty("sip-outbound-call")
        val sipOutboundCall: Boolean? = null,
        val transcription: Boolean? = null
    )

    companion object {
        private val mapper = jacksonObjectMapper().apply {
            enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
        }
        private val base64decoder = Base64.getUrlDecoder()

        /**
         * Parse a JWT into the JitsiToken structure without validating the signature.
         */
        @Throws(JsonProcessingException::class, JsonMappingException::class, IllegalArgumentException::class)
        fun parseWithoutValidation(string: String): JitsiToken = string.split(".").let {
            if (it.size >= 2) {
                parseJson(base64decoder.decode(it[1]).toString(Charsets.UTF_8))
            } else {
                throw IllegalArgumentException("Invalid JWT format")
            }
        }

        /**
         * Parse a JSON string into the JitsiToken structure.
         */
        @Throws(JsonProcessingException::class, JsonMappingException::class)
        fun parseJson(string: String): JitsiToken {
            return mapper.readValue(string)
        }
    }
}
