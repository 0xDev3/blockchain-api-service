package dev3.blockchainapiservice.features.payout.service

import dev3.blockchainapiservice.service.FixedScheduler
import java.util.concurrent.TimeUnit

class ManualFixedScheduler : FixedScheduler {

    private var command: Runnable? = null

    override fun scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit) {
        this.command = command
    }

    override fun shutdown() {}

    fun execute() = command?.run()
}
