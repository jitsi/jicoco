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

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

private val objectMapper = jacksonObjectMapper().apply {
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
}

/**
 * This is based on the format used by VoxImplant here, hence the encoding of certain numeric fields as strings:
 * https://voximplant.com/docs/guides/voxengine/websocket
 * The event/message shapes are documented in VoxImplant's protobuf definition:
 * https://github.com/voximplant/protobuf/blob/main/websockets.proto
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "event")
@JsonSubTypes(
    JsonSubTypes.Type(value = MediaEvent::class, name = "media"),
    JsonSubTypes.Type(value = PingEvent::class, name = "ping"),
    JsonSubTypes.Type(value = PongEvent::class, name = "pong"),
    JsonSubTypes.Type(value = StartEvent::class, name = "start"),
    JsonSubTypes.Type(value = StopEvent::class, name = "stop"),
    JsonSubTypes.Type(value = TranscriptionResultEvent::class, name = "transcription-result"),
    JsonSubTypes.Type(value = InfoEvent::class, name = "info"),
)
sealed class Event(val event: String) {
    fun toJson(): String = objectMapper.writeValueAsString(this)
    companion object {
        fun parse(s: String): Event = objectMapper.readValue(s, Event::class.java)
        fun parse(s: List<String>): List<Event> = s.map { objectMapper.readValue(it, Event::class.java) }
    }
}

data class MediaEvent(
    @JsonSerialize(using = Int2StringSerializer::class)
    @JsonDeserialize(using = String2IntDeserializer::class)
    val sequenceNumber: Int,
    val media: Media
) : Event("media")

data class StartEvent(
    @JsonSerialize(using = Int2StringSerializer::class)
    @JsonDeserialize(using = String2IntDeserializer::class)
    val sequenceNumber: Int,
    val start: Start
) : Event("start")

/**
 * The `stop` event, mirroring VoxImplant's (see the doc linked above): signals the end of a media stream and pairs
 * with a [StartEvent]. Its [Stop.timestamp] is an optional augmentation -- present when a peer uses start/stop to
 * bracket a contiguous run of media (a "talk") on the media timeline, absent for a plain stop.
 */
data class StopEvent(
    @JsonSerialize(using = Int2StringSerializer::class)
    @JsonDeserialize(using = String2IntDeserializer::class)
    val sequenceNumber: Int,
    val stop: Stop
) : Event("stop")

data class PingEvent(
    val id: Int
) : Event("ping")

data class PongEvent(
    val id: Int
) : Event("pong")

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(value = ["event"], allowGetters = false)
class TranscriptionResultEvent : Event("transcription-result") {
    private val additionalProperties = mutableMapOf<String, Any?>()

    @JsonAnySetter
    fun setAdditionalProperty(name: String, value: Any?) {
        additionalProperties[name] = value
    }

    @JsonAnyGetter
    fun getAdditionalProperties(): Map<String, Any?> = additionalProperties
}

class SessionEndEvent : Event("session-end")

/**
 * Declares the set of source names the bridge will export (send out) to the peer, and the set it requests (wants to
 * receive) from the peer. Only ever sent by the bridge -- like [SessionEndEvent] it is not registered in [Event]'s
 * subtypes for parsing.
 */
data class SourcesEvent(
    val exports: List<String>,
    val requests: List<String>
) : Event("sources")

/**
 * An informational message exchanged once when the connection is established, carrying
 * application/version/deployment/session metadata for runtime observability. It is a free-form
 * property bag (like [TranscriptionResultEvent]) so either side can add fields without a schema
 * change; unknown fields are preserved and can be logged by the receiver as-is.
 */
@JsonIgnoreProperties(value = ["event"], allowGetters = false)
class InfoEvent : Event("info") {
    private val additionalProperties = mutableMapOf<String, Any?>()

    @JsonAnySetter
    fun setAdditionalProperty(name: String, value: Any?) {
        additionalProperties[name] = value
    }

    @JsonAnyGetter
    fun getAdditionalProperties(): Map<String, Any?> = additionalProperties

    /** Set a property, returning this for chaining when building an event to send. */
    fun put(name: String, value: Any?): InfoEvent {
        additionalProperties[name] = value
        return this
    }
}

data class MediaFormat(
    val encoding: String,
    val sampleRate: Int,
    val channels: Int,
    var parameters: Map<String, String>? = null
)
data class Start(
    val tag: String,
    val mediaFormat: MediaFormat,
    val customParameters: CustomParameters? = null,
    val diarize: Boolean? = null,
    /**
     * Optional augmentation (not part of the base VoxImplant format): the RTP timestamp -- on the same media
     * timeline as the [Media] events' timestamps (e.g. 48000 Hz for Opus) -- of the first packet of a contiguous
     * run of media (a "talk"), when a peer uses start/stop to bracket one (paired with a [StopEvent]). Null when the
     * start event only announces the media format. Encoded as a string like [Media.timestamp].
     */
    @JsonSerialize(using = Long2StringSerializer::class)
    @JsonDeserialize(using = String2LongDeserializer::class)
    val timestamp: Long? = null
)

data class CustomParameters(
    val endpointId: String?
)

/**
 * The payload of a [StopEvent]. [tag] is the source; [mediaInfo] the VoxImplant-defined end-of-stream statistics
 * (optional). [timestamp] is an optional augmentation (absent in a plain stop): the RTP timestamp -- on the same
 * media timeline as [Media.timestamp] and [Start.timestamp] -- one past the end of a "talk", i.e. the timestamp the
 * next contiguous packet would carry (were there no intervening silence). So a talk spans [start, stop).
 */
data class Stop(
    val tag: String,
    val mediaInfo: MediaInfo? = null,
    @JsonSerialize(using = Long2StringSerializer::class)
    @JsonDeserialize(using = String2LongDeserializer::class)
    val timestamp: Long? = null
)

/**
 * VoxImplant end-of-stream statistics carried by a [Stop] event: [bytesSent] is the encoded size of the media
 * stream and [duration] its length in milliseconds. String-encoded like the other VoxImplant numeric fields.
 */
data class MediaInfo(
    @JsonSerialize(using = Long2StringSerializer::class)
    @JsonDeserialize(using = String2LongDeserializer::class)
    val bytesSent: Long,
    @JsonSerialize(using = Long2StringSerializer::class)
    @JsonDeserialize(using = String2LongDeserializer::class)
    val duration: Long
)

data class Media(
    val tag: String,
    @JsonSerialize(using = Int2StringSerializer::class)
    @JsonDeserialize(using = String2IntDeserializer::class)
    val chunk: Int,
    @JsonSerialize(using = Long2StringSerializer::class)
    @JsonDeserialize(using = String2LongDeserializer::class)
    val timestamp: Long,
    val payload: String,
    /**
     * The RFC 6464 audio level of this frame (0-127, expressed as -dBov, i.e. 0 is loudest and 127 is silence), as
     * reported by the sender in the ssrc-audio-level RTP header extension. Null when the sender did not include the
     * extension. Encoded as a natural JSON number (not a string), unlike the VoxImplant-derived numeric fields above.
     */
    val audioLevel: Int? = null,
    /**
     * The RFC 6464 Voice Activity Detection flag for this frame, as reported by the sender in the ssrc-audio-level RTP
     * header extension. Null when the sender did not include the extension.
     */
    val vad: Boolean? = null
)

class Int2StringSerializer : JsonSerializer<Int>() {
    override fun serialize(value: Int, gen: JsonGenerator, p: SerializerProvider) {
        gen.writeString(value.toString())
    }
}
class String2IntDeserializer : JsonDeserializer<Int>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Int {
        return p.readValueAs(Int::class.java).toInt()
    }
}
class Long2StringSerializer : JsonSerializer<Long>() {
    override fun serialize(value: Long, gen: JsonGenerator, p: SerializerProvider) {
        gen.writeString(value.toString())
    }
}
class String2LongDeserializer : JsonDeserializer<Long>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Long {
        return p.readValueAs(Long::class.java).toLong()
    }
}
