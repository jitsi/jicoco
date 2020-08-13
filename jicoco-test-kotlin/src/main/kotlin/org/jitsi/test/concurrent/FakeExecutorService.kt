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

import org.jitsi.test.time.FakeClock
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

/**
 * A fake [ExecutorService] which gives control over when submitted tasks are run
 * without requiring a separate thread.
 */
abstract class FakeExecutorService : ExecutorService {
    private var jobs = JobsTimeline()
    val clock: FakeClock = FakeClock()

    override fun execute(command: Runnable) {
        jobs.add(Job(command, clock.instant()))
    }

    override fun submit(task: Runnable): Future<*> {
        val job = Job(task, clock.instant())
        jobs.add(job)
        return EmptyCompletableFuture { job.cancelled = true }
    }

    fun runOne() {
        if (jobs.isNotEmpty()) {
            val job = jobs.removeAt(0)
            if (!job.cancelled) {
                job.run()
            } else {
                // Check for another job since this one had been cancelled
                runOne()
            }
        }
    }

    fun runAll() {
        while (jobs.isNotEmpty()) {
            runOne()
        }
    }
}

/**
 * A simple implementation of [CompletableFuture<Unit>] which allows passing
 * a handler to be invoked on cancellation.
 */
private class EmptyCompletableFuture(private val cancelHandler: () -> Unit) : CompletableFuture<Unit>() {
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        cancelHandler()
        return true
    }
}
