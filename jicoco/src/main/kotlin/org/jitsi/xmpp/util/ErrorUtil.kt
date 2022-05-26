/*
 * Copyright @ 2015 - present, 8x8 Inc
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
package org.jitsi.xmpp.util

import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.StanzaError

@JvmOverloads
fun createError(
    request: IQ,
    errorCondition: StanzaError.Condition,
    errorMessage: String? = null,
    extension: ExtensionElement? = null
) = createError(
    request,
    errorCondition,
    errorMessage,
    if (extension == null) emptyList() else listOf(extension)
)

/**
 * Create an error response for a given IQ request.
 *
 * @param request the request IQ for which the error response will be created.
 * @param errorCondition the XMPP error condition for the error response.
 * @param errorMessage optional error text message to be included in the error response.
 * @param extensions optional extensions to include as a children of the error element.
 *
 * @return an IQ which is an XMPP error response to given <tt>request</tt>.
 */
fun createError(
    request: IQ,
    errorCondition: StanzaError.Condition,
    errorMessage: String? = null,
    extensions: List<ExtensionElement>
): IQ {
    val error = StanzaError.getBuilder(errorCondition)
    errorMessage?.let { error.setDescriptiveEnText(it) }
    if (extensions.isNotEmpty()) {
        error.setExtensions(extensions)
    }

    return IQ.createErrorResponse(request, error.build())
}
