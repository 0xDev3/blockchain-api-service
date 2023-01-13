package dev3.blockchainapiservice.config

import dev3.blockchainapiservice.features.payout.service.AssetSnapshotQueueServiceImpl
import dev3.blockchainapiservice.features.payout.service.ManualFixedScheduler
import dev3.blockchainapiservice.service.ScheduledExecutorServiceProvider
import mu.KLogging
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestSchedulerConfiguration {

    companion object : KLogging()

    @Bean
    fun snapshotQueueScheduler() = ManualFixedScheduler()

    @Bean
    @Primary
    fun scheduledExecutorServiceProvider(
        snapshotQueueScheduler: ManualFixedScheduler
    ): ScheduledExecutorServiceProvider {
        logger.info { "Using manual schedulers for tests" }

        return mock {
            given(it.newSingleThreadScheduledExecutor(AssetSnapshotQueueServiceImpl.QUEUE_NAME))
                .willReturn(snapshotQueueScheduler)
        }
    }
}
