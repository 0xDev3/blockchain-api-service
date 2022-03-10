package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.model.result.UnsignedVerificationMessage
import com.ampnet.blockchainapiservice.repository.SignedVerificationMessageRepository
import com.ampnet.blockchainapiservice.repository.UnsignedVerificationMessageRepository
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

class VerificationServiceTest : TestBase() {

    @Test
    fun mustCreateUnsignedVerificationMessageWithCorrectWalletAddressAndDuration() {
        val uuid = UUID.randomUUID()
        val walletAddress = WalletAddress("123")
        val now = UtcDateTime(OffsetDateTime.parse("2022-01-01T00:00:00Z"))
        val validityDuration = Duration.ofDays(1L)

        val message = UnsignedVerificationMessage(
            id = uuid,
            walletAddress = walletAddress,
            createdAt = now,
            validUntil = now + validityDuration
        )

        val unsignedMessageRepository = mock<UnsignedVerificationMessageRepository>()

        suppose("unsigned verification message will be stored into database") {
            given(unsignedMessageRepository.store(message))
                .willReturn(message)
        }

        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID is returned") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp is returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(now)
        }

        val applicationProperties = suppose("validity duration is set in application properties") {
            ApplicationProperties().apply { verification.unsignedMessageValidity = validityDuration }
        }

        val signedMessageRepository = mock<SignedVerificationMessageRepository>()
        val service = VerificationServiceImpl(
            unsignedMessageRepository,
            signedMessageRepository,
            uuidProvider,
            utcDateTimeProvider,
            applicationProperties
        )

        verify("unsigned verification message is correctly created") {
            val result = service.createUnsignedVerificationMessage(walletAddress)

            assertThat(result).withMessage()
                .isEqualTo(message)
        }
    }
}
