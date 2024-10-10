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

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser

class MediaJsonTest : ShouldSpec() {
    val parser = JSONParser()

    init {
        val seq = 123
        val tag = "t"
        context("StartEvent") {
            val enc = "opus"
            val sampleRate = 48000
            val channels = 2
            val event = StartEvent(seq, Start(tag, MediaFormat(enc, sampleRate, channels)))

            context("Serializing") {
                val parsed = parser.parse(event.toJson())

                parsed.shouldBeInstanceOf<JSONObject>()
                parsed["event"] shouldBe "start"
                // intentionally encoded as a string
                parsed["sequenceNumber"] shouldBe seq.toString()
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
                val parsed = Event.parse(event.toJson())
                (parsed == event) shouldBe true
                (parsed === event) shouldBe false

                val parsedList = Event.parse(listOf(event.toJson(), event.toJson()))
                parsedList.shouldBeInstanceOf<List<Event>>()
                parsedList.size shouldBe 2
                parsedList[0] shouldBe event
                parsedList[1] shouldBe event
            }
        }
        context("MediaEvent") {
            val chunk = 213
            val timestamp = 0x1_0000_ffff
            val payload = "p"
            val event = MediaEvent(seq, Media(tag, chunk, timestamp, payload))

            context("Serializing") {
                val parsed = parser.parse(event.toJson())
                parsed.shouldBeInstanceOf<JSONObject>()
                parsed["event"] shouldBe "media"
                // intentionally encoded as a string
                parsed["sequenceNumber"] shouldBe seq.toString()
                val media = parsed["media"]
                media.shouldBeInstanceOf<JSONObject>()
                media["tag"] shouldBe tag
                // intentionally encoded as a string
                media["chunk"] shouldBe chunk.toString()
                // intentionally encoded as a string
                media["timestamp"] shouldBe timestamp.toString()
                media["payload"] shouldBe payload
            }
            context("Parsing") {
                val parsed = Event.parse(event.toJson())
                (parsed == event) shouldBe true
                (parsed === event) shouldBe false
            }
        }
        context("Parsing valid samples") {
            context("Start") {
                val parsed = Event.parse(
                    """
                    { 
                        "event": "start",
                        "sequenceNumber": "0",
                        "start": {
                            "tag": "incoming",
                            "mediaFormat": { 
                                "encoding": "audio/x-mulaw", 
                                "sampleRate": 8000, 
                                "channels": 1 
                            },
                            "customParameters": {
                                "text1":"12312"
                            }
                        }
                    }
                    """.trimIndent()
                )

                parsed.shouldBeInstanceOf<StartEvent>()
                parsed.event shouldBe "start"
                parsed.sequenceNumber shouldBe 0
                parsed.start.tag shouldBe "incoming"
                parsed.start.mediaFormat.encoding shouldBe "audio/x-mulaw"
                parsed.start.mediaFormat.sampleRate shouldBe 8000
                parsed.start.mediaFormat.channels shouldBe 1
            }
            context("Start with sequence number as int") {
                val parsed = Event.parse(
                    """
                    { 
                        "event": "start",
                        "sequenceNumber": 0,
                        "start": {
                            "tag": "incoming",
                            "mediaFormat": { 
                                "encoding": "audio/x-mulaw", 
                                "sampleRate": 8000, 
                                "channels": 1 
                            },
                            "customParameters": {
                                "text1":"12312"
                            }
                        }
                    }
                    """.trimIndent()
                )

                parsed.shouldBeInstanceOf<StartEvent>()
                parsed.sequenceNumber shouldBe 0
            }
            context("Media") {
                val parsed = Event.parse(
                    """
                    { 
                        "event": "media",
                        "sequenceNumber": "2", 
                        "media": { 
                            "tag": "incoming", 
                            "chunk": "1",    
                            "timestamp": "5",
                            "payload": "no+JhoaJjpzSHxAKBgYJ...=="
                        } 
                    }
                    """.trimIndent()
                )

                parsed.shouldBeInstanceOf<MediaEvent>()
                parsed.event shouldBe "media"
                parsed.sequenceNumber shouldBe 2
                parsed.media.tag shouldBe "incoming"
                parsed.media.chunk shouldBe 1
                parsed.media.timestamp shouldBe 5
                parsed.media.payload shouldBe "no+JhoaJjpzSHxAKBgYJ...=="
            }
            context("Media with seq/chunk/timestamp as numbers") {
                val parsed = Event.parse(
                    """
                    { 
                        "event": "media",
                        "sequenceNumber": 2, 
                        "media": { 
                            "tag": "incoming", 
                            "chunk": 1,    
                            "timestamp": 5,
                            "payload": "no+JhoaJjpzSHxAKBgYJ...=="
                        } 
                    }
                    """.trimIndent()
                )

                parsed.shouldBeInstanceOf<MediaEvent>()
                parsed.event shouldBe "media"
                parsed.sequenceNumber shouldBe 2
                parsed.media.tag shouldBe "incoming"
                parsed.media.chunk shouldBe 1
                parsed.media.timestamp shouldBe 5
                parsed.media.payload shouldBe "no+JhoaJjpzSHxAKBgYJ...=="
            }
        }
        context("Parsing invalid samples") {
            context("Invalid sequence number") {
                shouldThrow<InvalidFormatException> {
                    Event.parse(
                        """
                        { 
                            "event": "media",
                            "sequenceNumber": "not a number", 
                            "media": { 
                                "tag": "incoming", 
                                "chunk": "1",    
                                "timestamp": "5",
                                "payload": "no+JhoaJjpzSHxAKBgYJ...=="
                            } 
                        }
                        """.trimIndent()
                    )
                }
            }
            context("Invalid chunk") {
                shouldThrow<InvalidFormatException> {
                    Event.parse(
                        """
                        { 
                            "event": "media",
                            "sequenceNumber": "1", 
                            "media": { 
                                "tag": "incoming", 
                                "chunk": "not a number",    
                                "timestamp": "5",
                                "payload": "no+JhoaJjpzSHxAKBgYJ...=="
                            } 
                        }
                        """.trimIndent()
                    )
                }
            }
            context("Invalid timestamp") {
                shouldThrow<InvalidFormatException> {
                    Event.parse(
                        """
                        { 
                            "event": "media",
                            "sequenceNumber": "1", 
                            "media": { 
                                "tag": "incoming", 
                                "chunk": "1",    
                                "timestamp": "not a number",
                                "payload": "no+JhoaJjpzSHxAKBgYJ...=="
                            } 
                        }
                        """.trimIndent()
                    )
                }
            }
        }
    }
}
