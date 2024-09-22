/*
 * Copyright @ 2024 - present 8x8, Inc.
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
package org.jitsi.mediajson

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser

class MediaJsonTest : ShouldSpec() {
    val parser = JSONParser()
    init {
        val seq = "123"
        val tag = "t"
        context("StartEvent") {
            val enc = "opus"
            val sampleRate = 48000
            val channels = 2
            val event = StartEvent(seq, Start(tag, MediaFormat(enc, sampleRate, channels)))

            context("Serializing") {
                val parsed = parser.parse(event.toXml())

                parsed.shouldBeInstanceOf<JSONObject>()
                parsed["event"] shouldBe "start"
                parsed["sequenceNumber"] shouldBe seq
                val start = parsed["start"]
                start.shouldBeInstanceOf<JSONObject>()
                start["tag"] shouldBe tag
                val mediaFormat = start["mediaFormat"]
                mediaFormat.shouldBeInstanceOf<JSONObject>()
                mediaFormat["encoding"] shouldBe enc
                mediaFormat["sampleRate"] shouldBe sampleRate
                mediaFormat["channels"] shouldBe channels
            }
            context("Parsing") {
                val parsed = Event.parse(event.toXml())
                (parsed == event) shouldBe true
                (parsed === event) shouldBe false

                val parsedList = Event.parse(listOf(event.toXml(), event.toXml()))
                parsedList.shouldBeInstanceOf<List<Event>>()
                parsedList.size shouldBe 2
                parsedList[0] shouldBe event
                parsedList[1] shouldBe event
            }
        }
        context("MediaEvent") {
            val chunk = "213"
            val timestamp = "ts"
            val payload = "p"
            val event = MediaEvent(seq, Media(tag, chunk, timestamp, payload))

            context("Serializing") {
                val parsed = parser.parse(event.toXml())
                parsed.shouldBeInstanceOf<JSONObject>()
                parsed["event"] shouldBe "media"
                parsed["sequenceNumber"] shouldBe seq
                val media = parsed["media"]
                media.shouldBeInstanceOf<JSONObject>()
                media["tag"] shouldBe tag
                media["chunk"] shouldBe chunk
                media["timestamp"] shouldBe timestamp
                media["payload"] shouldBe payload
            }
            context("Parsing") {
                val parsed = Event.parse(event.toXml())
                (parsed == event) shouldBe true
                (parsed === event) shouldBe false
            }
        }
    }
}
