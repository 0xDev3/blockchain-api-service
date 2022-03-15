package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.exception.BadSignatureException
import com.ampnet.blockchainapiservice.exception.ExpiredValidationMessageException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.result.SignedVerificationMessage
import com.ampnet.blockchainapiservice.model.result.UnsignedVerificationMessage
import com.ampnet.blockchainapiservice.repository.SignedVerificationMessageRepository
import com.ampnet.blockchainapiservice.repository.UnsignedVerificationMessageRepository
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

class VerificationServiceTest : TestBase() {

    companion object {
        // this message was signed using Metamask
        private val SIGNED_MESSAGE = SignedVerificationMessage(
            id = UUID.fromString("7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73"),
            walletAddress = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21"),
            signature = "0x2601a91eed301102ca423ffc36e43b4dc096bb556ecfb83f508047b34ab7236f4cd1eaaae98ee8eac9cde62988" +
                "f062891f3c84e241d320cf338bdfc17a51bc131b",
            createdAt = UtcDateTime(OffsetDateTime.parse("2022-01-01T00:00:00Z")),
            verifiedAt = UtcDateTime(OffsetDateTime.parse("2022-01-01T01:00:00Z")),
            validUntil = UtcDateTime(OffsetDateTime.parse("2022-01-01T02:00:00Z"))
        )

        // also signed using Metamask, but using another address
        private const val OTHER_SIGNATURE = "0x4f6e4a316147dcc797a89cf6163643a5f0315dbed5fbf8976f2af1ada260468c53411d" +
            "b1d501550f171463322dbcf8427c9b34ff40513bec90d46b75b910b7c71b"
    }

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

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUnsignedMessageDoesNotExist() {
        val unsignedMessageRepository = mock<UnsignedVerificationMessageRepository>()

        suppose("unsigned verification message repository will return null") {
            given(unsignedMessageRepository.getById(any()))
                .willReturn(null)
        }

        val signedMessageRepository = mock<SignedVerificationMessageRepository>()
        val uuidProvider = mock<UuidProvider>()
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
        val applicationProperties = ApplicationProperties()
        val service = VerificationServiceImpl(
            unsignedMessageRepository,
            signedMessageRepository,
            uuidProvider,
            utcDateTimeProvider,
            applicationProperties
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.verifyAndStoreMessageSignature(SIGNED_MESSAGE.id, SIGNED_MESSAGE.signature)
            }
        }
    }

    @Test
    fun mustThrowExpiredValidationMessageExceptionWhenUnsignedMessageHasExpired() {
        val unsignedMessageRepository = mock<UnsignedVerificationMessageRepository>()
        val unsignedMessage = UnsignedVerificationMessage(
            id = SIGNED_MESSAGE.id,
            walletAddress = SIGNED_MESSAGE.walletAddress,
            createdAt = SIGNED_MESSAGE.createdAt,
            validUntil = SIGNED_MESSAGE.validUntil
        )

        suppose("unsigned verification message repository will return unsigned verification message") {
            given(unsignedMessageRepository.getById(SIGNED_MESSAGE.id))
                .willReturn(unsignedMessage)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("current time is after unsigned message validity time") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(SIGNED_MESSAGE.validUntil + Duration.ofMinutes(1L))
        }

        val signedMessageRepository = mock<SignedVerificationMessageRepository>()
        val uuidProvider = mock<UuidProvider>()
        val applicationProperties = ApplicationProperties()
        val service = VerificationServiceImpl(
            unsignedMessageRepository,
            signedMessageRepository,
            uuidProvider,
            utcDateTimeProvider,
            applicationProperties
        )

        verify("ExpiredValidationMessageException is thrown") {
            assertThrows<ExpiredValidationMessageException>(message) {
                service.verifyAndStoreMessageSignature(SIGNED_MESSAGE.id, SIGNED_MESSAGE.signature)
            }
        }
    }

    @Test
    fun mustThrowBadSignatureExceptionWhenMessageSignatureLengthIsInvalid() {
        val unsignedMessageRepository = mock<UnsignedVerificationMessageRepository>()
        val unsignedMessage = UnsignedVerificationMessage(
            id = SIGNED_MESSAGE.id,
            walletAddress = SIGNED_MESSAGE.walletAddress,
            createdAt = SIGNED_MESSAGE.createdAt,
            validUntil = SIGNED_MESSAGE.validUntil
        )

        suppose("unsigned verification message repository will return unsigned verification message") {
            given(unsignedMessageRepository.getById(SIGNED_MESSAGE.id))
                .willReturn(unsignedMessage)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("current time is before unsigned message validity time") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(SIGNED_MESSAGE.verifiedAt)
        }

        val signedMessageRepository = mock<SignedVerificationMessageRepository>()
        val uuidProvider = mock<UuidProvider>()
        val applicationProperties = ApplicationProperties()
        val service = VerificationServiceImpl(
            unsignedMessageRepository,
            signedMessageRepository,
            uuidProvider,
            utcDateTimeProvider,
            applicationProperties
        )

        verify("BadSignatureException is thrown") {
            assertThrows<BadSignatureException>(message) {
                service.verifyAndStoreMessageSignature(SIGNED_MESSAGE.id, "too short signature")
            }
        }
    }

    @Test
    fun mustThrowBadSignatureExceptionWhenMessageSignatureHasCorrectLengthAndInvalidFormat() {
        val unsignedMessageRepository = mock<UnsignedVerificationMessageRepository>()
        val unsignedMessage = UnsignedVerificationMessage(
            id = SIGNED_MESSAGE.id,
            walletAddress = SIGNED_MESSAGE.walletAddress,
            createdAt = SIGNED_MESSAGE.createdAt,
            validUntil = SIGNED_MESSAGE.validUntil
        )

        suppose("unsigned verification message repository will return unsigned verification message") {
            given(unsignedMessageRepository.getById(SIGNED_MESSAGE.id))
                .willReturn(unsignedMessage)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("current time is before unsigned message validity time") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(SIGNED_MESSAGE.verifiedAt)
        }

        val signedMessageRepository = mock<SignedVerificationMessageRepository>()
        val uuidProvider = mock<UuidProvider>()
        val applicationProperties = ApplicationProperties()
        val service = VerificationServiceImpl(
            unsignedMessageRepository,
            signedMessageRepository,
            uuidProvider,
            utcDateTimeProvider,
            applicationProperties
        )

        verify("BadSignatureException is thrown") {
            assertThrows<BadSignatureException>(message) {
                service.verifyAndStoreMessageSignature(
                    SIGNED_MESSAGE.id,
                    SIGNED_MESSAGE.signature.replace(".".toRegex(), "x")
                )
            }
        }
    }

    @Test
    fun mustThrowBadSignatureExceptionWhenMessageSignatureIsInValid() {
        val unsignedMessageRepository = mock<UnsignedVerificationMessageRepository>()
        val unsignedMessage = UnsignedVerificationMessage(
            id = SIGNED_MESSAGE.id,
            walletAddress = SIGNED_MESSAGE.walletAddress,
            createdAt = SIGNED_MESSAGE.createdAt,
            validUntil = SIGNED_MESSAGE.validUntil
        )

        suppose("unsigned verification message repository will return unsigned verification message") {
            given(unsignedMessageRepository.getById(SIGNED_MESSAGE.id))
                .willReturn(unsignedMessage)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("current time is before unsigned message validity time") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(SIGNED_MESSAGE.verifiedAt)
        }

        val signedMessageRepository = mock<SignedVerificationMessageRepository>()
        val uuidProvider = mock<UuidProvider>()
        val applicationProperties = ApplicationProperties()
        val service = VerificationServiceImpl(
            unsignedMessageRepository,
            signedMessageRepository,
            uuidProvider,
            utcDateTimeProvider,
            applicationProperties
        )

        verify("BadSignatureException is thrown") {
            assertThrows<BadSignatureException>(message) {
                service.verifyAndStoreMessageSignature(SIGNED_MESSAGE.id, OTHER_SIGNATURE)
            }
        }
    }

    @Test
    fun mustCorrectlyVerifyValidMessageSignature() {
        val unsignedMessageRepository = mock<UnsignedVerificationMessageRepository>()
        val unsignedMessage = UnsignedVerificationMessage(
            id = SIGNED_MESSAGE.id,
            walletAddress = SIGNED_MESSAGE.walletAddress,
            createdAt = SIGNED_MESSAGE.createdAt,
            validUntil = SIGNED_MESSAGE.validUntil
        )

        suppose("unsigned verification message repository will return unsigned verification message") {
            given(unsignedMessageRepository.getById(SIGNED_MESSAGE.id))
                .willReturn(unsignedMessage)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("current time is before unsigned message validity time") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(SIGNED_MESSAGE.verifiedAt)
        }

        suppose("unsigned verification message will be deleted by ID") {
            given(unsignedMessageRepository.deleteById(SIGNED_MESSAGE.id))
                .willReturn(true)
        }

        val signedMessageRepository = mock<SignedVerificationMessageRepository>()

        suppose("correct signed message is stored in database") {
            given(signedMessageRepository.store(SIGNED_MESSAGE))
                .willReturn(SIGNED_MESSAGE)
        }

        val uuidProvider = mock<UuidProvider>()
        val applicationProperties = ApplicationProperties().apply {
            verification.signedMessageValidity = Duration.ofHours(1L)
        }
        val service = VerificationServiceImpl(
            unsignedMessageRepository,
            signedMessageRepository,
            uuidProvider,
            utcDateTimeProvider,
            applicationProperties
        )

        verify("valid message is correctly verified") {
            val result = service.verifyAndStoreMessageSignature(SIGNED_MESSAGE.id, SIGNED_MESSAGE.signature)

            assertThat(result).withMessage()
                .isEqualTo(SIGNED_MESSAGE)
        }
    }
}
