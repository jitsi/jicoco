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

package org.jitsi.tracing

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context
import org.jitsi.xmpp.extensions.TraceParent
import org.jivesoftware.smack.packet.IQ

object TracingUtil {
    @JvmStatic
    fun remoteSpanFromIq(iq: IQ): Span? {
        val extension: TraceParent = iq.getExtension(TraceParent::class.java) ?: return null
        return Span.wrap(
            SpanContext.createFromRemoteParent(
                extension.traceId,
                extension.parentId,
                TraceFlags.fromHex(extension.traceFlags, 0),
                TraceState.getDefault()
            )
        )
    }

    @JvmStatic
    fun remoteContextFromIq(iq: IQ): Context {
        val root = Context.root()
        val span = remoteSpanFromIq(iq) ?: return root
        return root.with(span)
    }
}
