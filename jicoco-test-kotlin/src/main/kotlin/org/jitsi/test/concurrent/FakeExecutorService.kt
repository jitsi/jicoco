package org.jitsi.test.concurrent

import io.mockk.every
import io.mockk.mockk
import org.jitsi.test.time.FakeClock
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

abstract class FakeExecutorService : ExecutorService {
    private var jobs = JobsTimeline()
    val clock: FakeClock = FakeClock()

    override fun execute(command: Runnable) {
        jobs.add(Job(command, clock.instant()))
    }

    override fun submit(task: Runnable): Future<*> {
        val job = Job(task, clock.instant())
        val future: CompletableFuture<Unit> = mockk() {
            every { cancel(any()) } answers {
                job.cancelled = true
                true
            }
        }
        jobs.add(job)
        return future
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
