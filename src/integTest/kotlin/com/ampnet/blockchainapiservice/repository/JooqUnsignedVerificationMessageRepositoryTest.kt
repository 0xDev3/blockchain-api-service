package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UnsignedVerificationMessageRecord
import com.ampnet.blockchainapiservice.model.result.UnsignedVerificationMessage
import com.ampnet.blockchainapiservice.service.UtcDateTimeProvider
import com.ampnet.blockchainapiservice.testcontainers.PostgresTestContainer
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

@JooqTest
@Import(JooqUnsignedVerificationMessageRepository::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqUnsignedVerificationMessageRepositoryTest : TestBase() {

    companion object {
        private val WALLET_ADDRESS = WalletAddress("0")
        private val CREATED_AT = UtcDateTime(OffsetDateTime.parse("2022-01-01T00:00:00Z"))
        private val VALID_UNTIL = CREATED_AT + Duration.ofDays(1L)
    }

    @Suppress("unused")
    private val postgresContainer = PostgresTestContainer()

    @Autowired
    private lateinit var repository: JooqUnsignedVerificationMessageRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @MockBean
    private lateinit var utcDateTimeProvider: UtcDateTimeProvider

    @Test
    fun mustCorrectlyFetchUnsignedVerificationMessageById() {
        val id = UUID.randomUUID()

        suppose("some unsigned verification message exists in database") {
            dslContext.executeInsert(
                UnsignedVerificationMessageRecord(
                    id = id,
                    walletAddress = WALLET_ADDRESS.rawValue,
                    createdAt = CREATED_AT.value,
                    validUntil = VALID_UNTIL.value
                )
            )
        }

        verify("unsigned verification message is correctly fetched by ID") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    UnsignedVerificationMessage(
                        id = id,
                        walletAddress = WALLET_ADDRESS,
                        createdAt = CREATED_AT,
                        validUntil = VALID_UNTIL
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchNonExistentUnsignedVerificationMessageById() {
        verify("null is returned when fetching non-existent unsigned verification message") {
            val result = repository.getById(UUID.randomUUID())

            assertThat(result).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyStoreUnsignedVerificationMessage() {
        val message = suppose("some unsigned verification message is created") {
            createMessage()
        }

        val storedMessage = suppose("unsigned verification message is stored in database") {
            repository.store(message)
        }

        verify("storing unsigned verification message returns correct result") {
            assertThat(storedMessage).withMessage()
                .isEqualTo(message)
        }

        verify("unsigned verification message was stored in database") {
            val result = repository.getById(message.id)

            assertThat(result).withMessage()
                .isEqualTo(message)
        }
    }

    @Test
    fun mustCorrectlyDeleteUnsignedVerificationMessageById() {
        val id = UUID.randomUUID()

        suppose("some unsigned verification message exists in database") {
            dslContext.executeInsert(
                UnsignedVerificationMessageRecord(
                    id = id,
                    walletAddress = WALLET_ADDRESS.rawValue,
                    createdAt = CREATED_AT.value,
                    validUntil = VALID_UNTIL.value
                )
            )
        }

        val isDeleted = suppose("unsigned verification message is deleted by ID") {
            repository.deleteById(id)
        }

        verify("unsigned verification message was deleted from database") {
            assertThat(isDeleted).withMessage()
                .isTrue()

            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyDeleteAllExpiredUnsignedVerificationMessages() {
        val expirationTime = CREATED_AT + Duration.ofHours(1L)
        val expiredMessages = listOf(
            createMessage(validUntil = expirationTime),
            createMessage(validUntil = expirationTime - Duration.ofSeconds(30L)),
            createMessage(validUntil = expirationTime - Duration.ofMinutes(30L))
        )
        val nonExpiredMessages = listOf(
            createMessage(),
            createMessage()
        )

        suppose("some messages are stored in database") {
            expiredMessages.forEach { repository.store(it) }
            nonExpiredMessages.forEach { repository.store(it) }
        }

        suppose("current timestamp is equal to specified expiration time") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(expirationTime)
        }

        val numDeleted = suppose("expired messages are deleted from database") {
            repository.deleteAllExpired()
        }

        verify("correct number of messages was deleted") {
            assertThat(numDeleted).withMessage()
                .isEqualTo(expiredMessages.size)
        }

        verify("expired messages are deleted from database") {
            expiredMessages.forEach {
                val result = repository.getById(it.id)

                assertThat(result).withMessage()
                    .isNull()
            }
        }

        verify("non-expired messages are still in database") {
            nonExpiredMessages.forEach {
                val result = repository.getById(it.id)

                assertThat(result).withMessage()
                    .isEqualTo(it)
            }
        }
    }

    private fun createMessage(validUntil: UtcDateTime = VALID_UNTIL): UnsignedVerificationMessage =
        UnsignedVerificationMessage(
            id = UUID.randomUUID(),
            walletAddress = WALLET_ADDRESS,
            createdAt = CREATED_AT,
            validUntil = validUntil
        )
}
