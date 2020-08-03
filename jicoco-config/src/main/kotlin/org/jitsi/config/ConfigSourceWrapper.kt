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

package org.jitsi.config

import org.jitsi.metaconfig.ConfigSource
import kotlin.reflect.KType

/**
 * A wrapper around a [ConfigSource] that allows changing the underlying
 * [ConfigSource] at any time.  We use this in [JitsiConfig] so that test
 * code can swap out the underlying [ConfigSource].
 */
class ConfigSourceWrapper(
    var innerSource: ConfigSource
) : ConfigSource {

    override val name: String
        get() = innerSource.name

    override fun getterFor(type: KType): (String) -> Any =
        innerSource.getterFor(type)
}
