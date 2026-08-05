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

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import org.jitsi.xmpp.extensions.TraceParent
import org.jivesoftware.smack.packet.IQ

private class TestIq : IQ("test", "test:namespace") {
    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder): IQChildElementXmlStringBuilder {
        xml.setEmptyElement()
        return xml
    }
}

class TracingUtilTest : ShouldSpec() {
    init {
        val traceId = "4bf92f3577b34da6a3ce929d0e0e4736"
        val parentId = "00f067aa0ba902b7"
        val traceFlags = "01"

        context("remoteSpanFromIq") {
            should("return null when the IQ has no TraceParent extension") {
                TracingUtil.remoteSpanFromIq(TestIq()).shouldBeNull()
            }
            should("build a span from the IQ's TraceParent extension") {
                val iq = TestIq().apply { addExtension(TraceParent(traceId, parentId, traceFlags)) }

                val span = TracingUtil.remoteSpanFromIq(iq)

                span.shouldNotBeNull()
                val context = span.spanContext
                context.traceId shouldBe traceId
                context.spanId shouldBe parentId
                context.isSampled shouldBe true
            }
        }

        context("remoteContextFromIq") {
            should("return the root context when the IQ has no TraceParent extension") {
                TracingUtil.remoteContextFromIq(TestIq()) shouldBe Context.root()
            }
            should("wrap the remote span when the IQ has a TraceParent extension") {
                val iq = TestIq().apply { addExtension(TraceParent(traceId, parentId, traceFlags)) }

                val context = TracingUtil.remoteContextFromIq(iq)

                Span.fromContext(context).spanContext.traceId shouldBe traceId
            }
        }
    }
}
