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

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonProcessingException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.*

class JitsiTokenTest : ShouldSpec({
    context("parsing a meet-jit-si firebase token") {
        JitsiToken.parseJson(meetJitsiJson).apply {
            this shouldNotBe null
            name shouldBe "Boris Grozev"
            picture shouldBe "https://example.com/avatar.png"
            iss shouldBe "issuer"
            aud shouldBe "audience"
            userId shouldBe "user_id"
            sub shouldBe "sub"
            iat shouldBe 1748367729
            exp shouldBe 1748371329
            email shouldBe "boris@example.com"
            emailVerified shouldBe true
            context shouldBe null
        }
    }
    context("parsing an 8x8.vc token") {
        JitsiToken.parseJson(prodJson).apply {
            this shouldNotBe null
            aud shouldBe "jitsi"
            iss shouldBe "chat"
            exp shouldBe 1748454424
            nbf shouldBe 1748368024
            room shouldBe ""
            sub shouldBe ""

            context shouldNotBe null
            context?.group shouldBe "54321"
            context?.tenant shouldBe "tenant"

            context?.user shouldNotBe null
            context?.user?.id shouldBe "12345"
            context?.user?.name shouldBe "Boris Grozev"
            context?.user?.avatar shouldBe "https://example.com/avatar.png"
            context?.user?.email shouldBe "Boris@example.com"

            context?.features shouldNotBe null
            context?.features?.flip shouldBe true
            context?.features?.livestreaming shouldBe true
            context?.features?.outboundCall shouldBe false
            context?.features?.recording shouldBe false
            context?.features?.sipOutboundCall shouldBe true
            context?.features?.transcription shouldBe null
        }
    }
    context("parsing an invalid token") {
        shouldThrow<JsonProcessingException> {
            JitsiToken.parseJson("{ invalid ")
        }
    }
    context("parsing a JWT") {
        listOf(meetJitsiJson, prodJson).forEach { json ->
            val jwtNoSig = "${header.base64Encode()}.${json.base64Encode()}"
            JitsiToken.parseWithoutValidation(jwtNoSig) shouldBe JitsiToken.parseJson(json)

            val jwt = "$jwtNoSig.signature"
            JitsiToken.parseWithoutValidation(jwt) shouldBe JitsiToken.parseJson(json)
        }
    }
    context("parsing an invalid JWT") {
        shouldThrow<IllegalArgumentException> {
            JitsiToken.parseWithoutValidation("invalid")
        }
        shouldThrow<IllegalArgumentException> {
            JitsiToken.parseWithoutValidation("invalid.%")
        }
        shouldThrow<JsonParseException> {
            JitsiToken.parseWithoutValidation("invalid.jwt")
        }
    }
})

private val meetJitsiJson = """
{
  "name": "Boris Grozev",
  "picture": "https://example.com/avatar.png",
  "iss": "issuer",
  "aud": "audience",
  "auth_time": 1731944223,
  "user_id": "user_id",
  "sub": "sub",
  "iat": 1748367729,
  "exp": 1748371329,
  "email": "boris@example.com",
  "email_verified": true,
  "firebase": {
    "identities": {
      "google.com": [
        "1234"
      ],
      "email": [
        "boris@example.com"
      ]
    },
    "sign_in_provider": "google.com"
  }
}
""".trimIndent()

private val prodJson = """
{
  "aud": "jitsi",
  "context": {
    "user": {
      "id": "12345",
      "name": "Boris Grozev",
      "avatar": "https://example.com/avatar.png",
      "email": "Boris@example.com"
    },
    "group": "54321",
    "tenant": "tenant",
    "features": {
      "flip": "true",
      "livestreaming": true,
      "outbound-call": "false",
      "recording": false,
      "sip-outbound-call": "true"
    }
  },
  "exp": 1748454424,
  "iss": "chat",
  "nbf": 1748368024,
  "room": "",
  "sub": ""
}
""".trimIndent()

private val header = """{"alg":"RS256","kid":"kid","typ":"JWT"""".trimIndent()

private fun String.base64Encode(): String {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(this.toByteArray())
}
