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

package org.jitsi.shutdown

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@ExperimentalTime
class ShutdownServiceImplTest : ShouldSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val shutdownService = ShutdownServiceImpl()

    context("beginning shutdown") {
        should("notify waiters").config(timeout = Duration.seconds(5)) {
            val result = async {
                shutdownService.waitForShutdown()
                true
            }
            shutdownService.beginShutdown()
            result.await() shouldBe true
        }
    }
    context("waiting after shutdown is done") {
        shutdownService.beginShutdown()
        should("return 'immediately'").config(timeout = Duration.milliseconds(500)) {
            shutdownService.waitForShutdown()
        }
    }
})
