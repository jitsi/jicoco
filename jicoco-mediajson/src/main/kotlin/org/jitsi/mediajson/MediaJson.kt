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
}

/**
 * This is based on the format used by VoxImplant here, hence the encoding of certain numeric fields as strings:
 * https://voximplant.com/docs/guides/voxengine/websocket
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "event")
@JsonSubTypes(
    JsonSubTypes.Type(value = MediaEvent::class, name = "media"),
    JsonSubTypes.Type(value = PingEvent::class, name = "ping"),
    JsonSubTypes.Type(value = PongEvent::class, name = "pong"),
    JsonSubTypes.Type(value = StartEvent::class, name = "start"),
    JsonSubTypes.Type(value = TranscriptionResultEvent::class, name = "transcription-result"),
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

data class MediaFormat(
    val encoding: String,
    val sampleRate: Int,
    val channels: Int,
    var parameters: Map<String, String>? = null
)
data class Start(
    val tag: String,
    val mediaFormat: MediaFormat,
    val customParameters: CustomParameters? = null
)

data class CustomParameters(
    val endpointId: String?
)

data class Media(
    val tag: String,
    @JsonSerialize(using = Int2StringSerializer::class)
    @JsonDeserialize(using = String2IntDeserializer::class)
    val chunk: Int,
    @JsonSerialize(using = Long2StringSerializer::class)
    @JsonDeserialize(using = String2LongDeserializer::class)
    val timestamp: Long,
    val payload: String
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
