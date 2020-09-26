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

package org.jitsi.test.concurrent

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import org.jitsi.utils.secs
import java.util.concurrent.TimeUnit

class FakeScheduledExecutorServiceTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        val executor: FakeScheduledExecutorService = spyk()

        context("Scheduling a recurring job") {
            var numJobRuns = 0
            val handle = executor.scheduleAtFixedRate(
                {
                    numJobRuns++
                },
                5, 5, TimeUnit.SECONDS
            )
            context("and then calling runOne") {
                executor.runOne()
                should("have run the job") {
                    numJobRuns shouldBe 1
                }
                context("and then calling runOne again") {
                    executor.runOne()
                    should("have run the job again") {
                        numJobRuns shouldBe 2
                    }
                }
                context("and then cancelling the job") {
                    handle.cancel(true)
                    context("and then calling runOne again") {
                        executor.runOne()
                        should("not have run the job again") {
                            numJobRuns shouldBe 1
                        }
                    }
                }
            }
            context("and elapsing time before it should run") {
                executor.clock.elapse(4.secs)
                executor.run()
                should("not have run the job") {
                    numJobRuns shouldBe 0
                }
                context("and then elapsing past the scheduled time") {
                    executor.clock.elapse(3.secs)
                    executor.run()
                    should("have run the job") {
                        numJobRuns shouldBe 1
                    }
                }
            }
            context("and elapsing the time past multiple runs") {
                executor.clock.elapse(10.secs)
                executor.run()
                should("have run the job multiple times") {
                    numJobRuns shouldBe 2
                }
            }
        }
        context("scheduling a fixed rate job") {
            var numJobRuns = 0
            executor.scheduleAtFixedRate(
                {
                    numJobRuns++
                    // Elapse time inside the job to simulate a long job
                    executor.clock.elapse(3.secs)
                },
                5, 5, TimeUnit.SECONDS
            )
            should("run the job at a fixed rate") {
                // Run the first job
                executor.runOne()
                // Elapse long enough that the job should be run again, even though it's been less
                // than the period since the last run
                executor.clock.elapse(3.secs)
                executor.runUntil(executor.clock.instant())
                numJobRuns shouldBe 2
            }
        }
        context("scheduled a fixed delay job") {
            var numJobRuns = 0
            executor.scheduleWithFixedDelay(
                {
                    numJobRuns++
                    // Elapse time inside the job to simulate a long job
                    executor.clock.elapse(3.secs)
                },
                5, 5, TimeUnit.SECONDS
            )
            should("run the job with a fixed rate") {
                // Run the first job
                executor.runOne()
                // Elapse long enough that the job would've run if it were at fixed rate,
                // but shouldn't since it's fixed delay
                executor.clock.elapse(3.secs)
                executor.runUntil(executor.clock.instant())
                numJobRuns shouldBe 1
            }
        }
    }
}
