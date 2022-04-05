package no.pto.config

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Scheduler(private val task: Runnable) {
    private val executor = Executors.newScheduledThreadPool(1)

    fun scheduleExecution(every: Every) {
        val taskWrapper = Runnable {
            task.run()
        }
        executor.scheduleWithFixedDelay(taskWrapper, 1, every.n, every.unit)
    }


    fun stop() {
        executor.shutdown()
        try {
            executor.awaitTermination(1, TimeUnit.HOURS)
        } catch (e: InterruptedException) {
        }

    }
}

data class Every(val n: Long, val unit: TimeUnit)