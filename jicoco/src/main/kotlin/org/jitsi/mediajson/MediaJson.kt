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

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

private val objectMapper = jacksonObjectMapper()

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "event")
@JsonSubTypes(
    JsonSubTypes.Type(value = MediaEvent::class, name = "media"),
    JsonSubTypes.Type(value = StartEvent::class, name = "start"),
)
sealed class Event(val event: String) {
    fun toXml(): String = objectMapper.writeValueAsString(this)
    companion object {
        fun parse(s: String): Event = objectMapper.readValue(s, Event::class.java)
        fun parse(s: List<String>): List<Event> = s.map { objectMapper.readValue(it, Event::class.java) }
    }
}

data class MediaEvent(
    val sequenceNumber: String,
    val media: Media
) : Event("media")

data class StartEvent(
    val sequenceNumber: String,
    val start: Start
) : Event("start")

data class MediaFormat(
    val encoding: String,
    val sampleRate: Int,
    val channels: Int
)
data class Start(
    val tag: String,
    val mediaFormat: MediaFormat
)

data class Media(
    val tag: String,
    val chunk: String,
    val timestamp: String,
    val payload: String
)
