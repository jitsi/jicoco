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
