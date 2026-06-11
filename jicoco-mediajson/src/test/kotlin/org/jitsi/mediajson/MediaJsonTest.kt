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
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class MediaJsonTest : ShouldSpec() {
    val mapper = jacksonObjectMapper()

    init {
        val seq = 123
        val tag = "t"
        context("StartEvent") {
            val enc = "opus"
            val sampleRate = 48000
            val channels = 2
            val params = mapOf("k1" to "v2", "k2" to "v2")
            val event = StartEvent(seq, Start(tag, MediaFormat(enc, sampleRate, channels, params)))

            context("Serializing") {
                val parsed = mapper.readTree(event.toJson())

                parsed.shouldBeInstanceOf<ObjectNode>()
                parsed.get("event").asText() shouldBe "start"
                // intentionally encoded as a string
                parsed.get("sequenceNumber").asText() shouldBe seq.toString()
                val start = parsed.get("start")
                start.shouldBeInstanceOf<ObjectNode>()
                start.get("tag").asText() shouldBe tag
                start.get("customParameters") shouldBe null
                val mediaFormat = start.get("mediaFormat")
                mediaFormat.shouldBeInstanceOf<ObjectNode>()
                mediaFormat.get("encoding").asText() shouldBe enc
                mediaFormat.get("sampleRate").asInt() shouldBe sampleRate
                mediaFormat.get("channels").asInt() shouldBe channels
                val parsedParams = mediaFormat.get("parameters")
                parsedParams.shouldBeInstanceOf<ObjectNode>()
                params.forEach { (k, v) -> parsedParams.get(k).asText() shouldBe v }
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
                val parsed = mapper.readTree(event.toJson())
                parsed.shouldBeInstanceOf<ObjectNode>()
                parsed.get("event").asText() shouldBe "media"
                // intentionally encoded as a string
                parsed.get("sequenceNumber").asText() shouldBe seq.toString()
                val media = parsed.get("media")
                media.shouldBeInstanceOf<ObjectNode>()
                media.get("tag").asText() shouldBe tag
                // intentionally encoded as a string
                media.get("chunk").asText() shouldBe chunk.toString()
                // intentionally encoded as a string
                media.get("timestamp").asText() shouldBe timestamp.toString()
                media.get("payload").asText() shouldBe payload
            }
            context("Parsing") {
                val parsed = Event.parse(event.toJson())
                (parsed == event) shouldBe true
                (parsed === event) shouldBe false
            }
        }
        context("PingEvent") {
            val id = 42
            val event = PingEvent(id)

            context("Serializing") {
                val parsed = mapper.readTree(event.toJson())
                parsed.shouldBeInstanceOf<ObjectNode>()
                parsed.get("event").asText() shouldBe "ping"
                parsed.get("id").asInt() shouldBe id
            }
            context("Parsing") {
                val parsed = Event.parse(event.toJson())
                (parsed == event) shouldBe true
                (parsed === event) shouldBe false
            }
        }
        context("PongEvent") {
            val id = 123
            val event = PongEvent(id)

            context("Serializing") {
                val parsed = mapper.readTree(event.toJson())
                parsed.shouldBeInstanceOf<ObjectNode>()
                parsed.get("event").asText() shouldBe "pong"
                parsed.get("id").asInt() shouldBe id
            }
            context("Parsing") {
                val parsed = Event.parse(event.toJson())
                (parsed == event) shouldBe true
                (parsed === event) shouldBe false
            }
        }
        context("TranscriptionResultEvent") {
            val event = TranscriptionResultEvent()

            context("Serializing") {
                val parsed = mapper.readTree(event.toJson())
                parsed.shouldBeInstanceOf<ObjectNode>()
                parsed.get("event").asText() shouldBe "transcription-result"
            }
            context("Parsing") {
                val parsed = Event.parse(event.toJson())
                parsed.shouldBeInstanceOf<TranscriptionResultEvent>()
                parsed.event shouldBe "transcription-result"
            }
        }
        context("SourcesEvent") {
            val event = SourcesEvent(
                exports = listOf("523834112-a0", "2394a3432-a0"),
                requests = listOf("523834112-a0.en", "2394a3432-a0.hi")
            )

            context("Serializing") {
                val parsed = mapper.readTree(event.toJson())
                parsed.shouldBeInstanceOf<ObjectNode>()
                parsed.get("event").asText() shouldBe "sources"
                val exports = parsed.get("exports")
                exports.shouldBeInstanceOf<ArrayNode>()
                exports.map { it.asText() } shouldBe listOf("523834112-a0", "2394a3432-a0")
                val requests = parsed.get("requests")
                requests.shouldBeInstanceOf<ArrayNode>()
                requests.map { it.asText() } shouldBe listOf("523834112-a0.en", "2394a3432-a0.hi")
            }

            context("With empty lists") {
                val parsed = mapper.readTree(SourcesEvent(emptyList(), emptyList()).toJson())
                parsed.shouldBeInstanceOf<ObjectNode>()
                parsed.get("event").asText() shouldBe "sources"
                parsed.get("exports").shouldBeInstanceOf<ArrayNode>().size() shouldBe 0
                parsed.get("requests").shouldBeInstanceOf<ArrayNode>().size() shouldBe 0
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
                                "text1":"12312",
                                "endpointId": "abcdabcd"
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
                parsed.start.customParameters.shouldNotBeNull()
                parsed.start.customParameters?.endpointId shouldBe "abcdabcd"
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
                                "text1":"12312",
                                "endpointId":"abcdabcd"
                            }
                        }
                    }
                    """.trimIndent()
                )

                parsed.shouldBeInstanceOf<StartEvent>()
                parsed.sequenceNumber shouldBe 0
                parsed.start.customParameters.shouldNotBeNull()
                parsed.start.customParameters?.endpointId shouldBe "abcdabcd"
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
            context("Ping") {
                val parsed = Event.parse(
                    """
                    {
                        "event": "ping",
                        "id": 42
                    }
                    """.trimIndent()
                )

                parsed.shouldBeInstanceOf<PingEvent>()
                parsed.event shouldBe "ping"
                parsed.id shouldBe 42
            }
            context("Pong") {
                val parsed = Event.parse(
                    """
                    {
                        "event": "pong",
                        "id": 123
                    }
                    """.trimIndent()
                )

                parsed.shouldBeInstanceOf<PongEvent>()
                parsed.event shouldBe "pong"
                parsed.id shouldBe 123
            }
            context("TranscriptionResult ") {
                val originalJson = """
                    {
                        "transcript": [
                            {
                                "confidence": 0.999666973709317,
                                "text": "blah blah blah"
                            }
                        ],
                        "is_interim": false,
                        "message_id": "item_CnopdEudFcwXCkZfCIHrC",
                        "type": "transcription-result",
                        "event": "transcription-result",
                        "participant": {
                            "id": "08847b00",
                            "ssrc": "1776301157"
                        },
                        "timestamp": 1765989508172
                    }
                """.trimIndent()

                val parsed = Event.parse(originalJson)
                parsed.shouldBeInstanceOf<TranscriptionResultEvent>()

                val serialized = parsed.toJson()
                val reparsed = mapper.readTree(serialized)
                reparsed.shouldBeInstanceOf<ObjectNode>()

                reparsed.get("event").asText() shouldBe "transcription-result"
                reparsed.get("type").asText() shouldBe "transcription-result"
                reparsed.get("message_id").asText() shouldBe "item_CnopdEudFcwXCkZfCIHrC"
                reparsed.get("is_interim").asBoolean() shouldBe false
                reparsed.get("timestamp").asLong() shouldBe 1765989508172L

                val transcript = reparsed.get("transcript")
                transcript.shouldBeInstanceOf<ArrayNode>()
                transcript.size() shouldBe 1
                val transcriptItem = transcript[0]
                transcriptItem.shouldBeInstanceOf<ObjectNode>()
                transcriptItem.get("confidence").asDouble() shouldBe 0.999666973709317
                transcriptItem.get("text").asText() shouldBe "blah blah blah"

                val participant = reparsed.get("participant")
                participant.shouldBeInstanceOf<ObjectNode>()
                participant.get("id").asText() shouldBe "08847b00"
                participant.get("ssrc").asText() shouldBe "1776301157"
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
