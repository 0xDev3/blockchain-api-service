package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.TestBase

@Deprecated("SD-771")
class VerificationServiceTest : TestBase() {

//    @Test
//    fun mustCreateUnsignedVerificationMessageWithCorrectWalletAddressAndDuration() {
//        val uuid = UUID.randomUUID()
//        val walletAddress = WalletAddress("123")
//        val now = UtcDateTime(OffsetDateTime.parse("2022-01-01T00:00:00Z"))
//        val validityDuration = Duration.ofDays(1L)
//
//        val message = UnsignedVerificationMessage(
//            id = uuid,
//            walletAddress = walletAddress,
//            createdAt = now,
//            validUntil = now + validityDuration
//        )
//
//        val unsignedMessageRepository = mock<UnsignedVerificationMessageRepository>()
//
//        suppose("unsigned verification message will be stored into database") {
//            given(unsignedMessageRepository.store(message))
//                .willReturn(message)
//        }
//
//        val uuidProvider = mock<UuidProvider>()
//
//        suppose("some UUID is returned") {
//            given(uuidProvider.getUuid())
//                .willReturn(uuid)
//        }
//
//        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
//
//        suppose("some timestamp is returned") {
//            given(utcDateTimeProvider.getUtcDateTime())
//                .willReturn(now)
//        }
//
//        val applicationProperties = suppose("validity duration is set in application properties") {
//            ApplicationProperties().apply { verification.unsignedMessageValidity = validityDuration }
//        }
//
//        val signedMessageRepository = mock<SignedVerificationMessageRepository>()
//        val service = VerificationServiceImpl(
//            unsignedMessageRepository,
//            signedMessageRepository,
//            uuidProvider,
//            utcDateTimeProvider,
//            applicationProperties
//        )
//
//        verify("unsigned verification message is correctly created") {
//            val result = service.createUnsignedVerificationMessage(walletAddress)
//
//            assertThat(result).withMessage()
//                .isEqualTo(message)
//        }
//    }
//
//    @Test
//    fun mustThrowResourceNotFoundExceptionWhenUnsignedMessageDoesNotExist() {
//        val unsignedMessageRepository = mock<UnsignedVerificationMessageRepository>()
//
//        suppose("unsigned verification message repository will return null") {
//            given(unsignedMessageRepository.getById(any()))
//                .willReturn(null)
//        }
//
//        val signedMessageRepository = mock<SignedVerificationMessageRepository>()
//        val uuidProvider = mock<UuidProvider>()
//        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
//        val applicationProperties = ApplicationProperties()
//        val service = VerificationServiceImpl(
//            unsignedMessageRepository,
//            signedMessageRepository,
//            uuidProvider,
//            utcDateTimeProvider,
//            applicationProperties
//        )
//
//        verify("ResourceNotFoundException is thrown") {
//            assertThrows<ResourceNotFoundException>(message) {
//                service.verifyAndStoreMessageSignature(TestData.SIGNED_MESSAGE.id, TestData.SIGNED_MESSAGE.signature)
//            }
//        }
//    }
//
//    @Test
//    fun mustThrowExpiredValidationMessageExceptionWhenUnsignedMessageHasExpired() {
//        val unsignedMessageRepository = mock<UnsignedVerificationMessageRepository>()
//
//        suppose("unsigned verification message repository will return unsigned verification message") {
//            given(unsignedMessageRepository.getById(TestData.SIGNED_MESSAGE.id))
//                .willReturn(TestData.UNSIGNED_MESSAGE)
//        }
//
//        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
//
//        suppose("current time is after unsigned message validity time") {
//            given(utcDateTimeProvider.getUtcDateTime())
//                .willReturn(TestData.SIGNED_MESSAGE.validUntil + Duration.ofMinutes(1L))
//        }
//
//        val signedMessageRepository = mock<SignedVerificationMessageRepository>()
//        val uuidProvider = mock<UuidProvider>()
//        val applicationProperties = ApplicationProperties()
//        val service = VerificationServiceImpl(
//            unsignedMessageRepository,
//            signedMessageRepository,
//            uuidProvider,
//            utcDateTimeProvider,
//            applicationProperties
//        )
//
//        verify("ExpiredValidationMessageException is thrown") {
//            assertThrows<ExpiredValidationMessageException>(message) {
//                service.verifyAndStoreMessageSignature(TestData.SIGNED_MESSAGE.id, TestData.SIGNED_MESSAGE.signature)
//            }
//        }
//    }
//
//    @Test
//    fun mustThrowBadSignatureExceptionWhenMessageSignatureLengthIsInvalid() {
//        val unsignedMessageRepository = mock<UnsignedVerificationMessageRepository>()
//
//        suppose("unsigned verification message repository will return unsigned verification message") {
//            given(unsignedMessageRepository.getById(TestData.SIGNED_MESSAGE.id))
//                .willReturn(TestData.UNSIGNED_MESSAGE)
//        }
//
//        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
//
//        suppose("current time is before unsigned message validity time") {
//            given(utcDateTimeProvider.getUtcDateTime())
//                .willReturn(TestData.SIGNED_MESSAGE.verifiedAt)
//        }
//
//        val signedMessageRepository = mock<SignedVerificationMessageRepository>()
//        val uuidProvider = mock<UuidProvider>()
//        val applicationProperties = ApplicationProperties()
//        val service = VerificationServiceImpl(
//            unsignedMessageRepository,
//            signedMessageRepository,
//            uuidProvider,
//            utcDateTimeProvider,
//            applicationProperties
//        )
//
//        verify("BadSignatureException is thrown") {
//            assertThrows<BadSignatureException>(message) {
//                service.verifyAndStoreMessageSignature(TestData.SIGNED_MESSAGE.id, TestData.TOO_SHORT_SIGNATURE)
//            }
//        }
//    }
//
//    @Test
//    fun mustThrowBadSignatureExceptionWhenMessageSignatureHasCorrectLengthAndInvalidFormat() {
//        val unsignedMessageRepository = mock<UnsignedVerificationMessageRepository>()
//
//        suppose("unsigned verification message repository will return unsigned verification message") {
//            given(unsignedMessageRepository.getById(TestData.SIGNED_MESSAGE.id))
//                .willReturn(TestData.UNSIGNED_MESSAGE)
//        }
//
//        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
//
//        suppose("current time is before unsigned message validity time") {
//            given(utcDateTimeProvider.getUtcDateTime())
//                .willReturn(TestData.SIGNED_MESSAGE.verifiedAt)
//        }
//
//        val signedMessageRepository = mock<SignedVerificationMessageRepository>()
//        val uuidProvider = mock<UuidProvider>()
//        val applicationProperties = ApplicationProperties()
//        val service = VerificationServiceImpl(
//            unsignedMessageRepository,
//            signedMessageRepository,
//            uuidProvider,
//            utcDateTimeProvider,
//            applicationProperties
//        )
//
//        verify("BadSignatureException is thrown") {
//            assertThrows<BadSignatureException>(message) {
//                service.verifyAndStoreMessageSignature(TestData.SIGNED_MESSAGE.id, TestData.INVALID_SIGNATURE)
//            }
//        }
//    }
//
//    @Test
//    fun mustThrowBadSignatureExceptionWhenMessageSignatureIsInValid() {
//        val unsignedMessageRepository = mock<UnsignedVerificationMessageRepository>()
//
//        suppose("unsigned verification message repository will return unsigned verification message") {
//            given(unsignedMessageRepository.getById(TestData.SIGNED_MESSAGE.id))
//                .willReturn(TestData.UNSIGNED_MESSAGE)
//        }
//
//        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
//
//        suppose("current time is before unsigned message validity time") {
//            given(utcDateTimeProvider.getUtcDateTime())
//                .willReturn(TestData.SIGNED_MESSAGE.verifiedAt)
//        }
//
//        val signedMessageRepository = mock<SignedVerificationMessageRepository>()
//        val uuidProvider = mock<UuidProvider>()
//        val applicationProperties = ApplicationProperties()
//        val service = VerificationServiceImpl(
//            unsignedMessageRepository,
//            signedMessageRepository,
//            uuidProvider,
//            utcDateTimeProvider,
//            applicationProperties
//        )
//
//        verify("BadSignatureException is thrown") {
//            assertThrows<BadSignatureException>(message) {
//                service.verifyAndStoreMessageSignature(TestData.SIGNED_MESSAGE.id, TestData.OTHER_SIGNATURE)
//            }
//        }
//    }
//
//    @Test
//    fun mustCorrectlyVerifyValidMessageSignature() {
//        val unsignedMessageRepository = mock<UnsignedVerificationMessageRepository>()
//
//        suppose("unsigned verification message repository will return unsigned verification message") {
//            given(unsignedMessageRepository.getById(TestData.SIGNED_MESSAGE.id))
//                .willReturn(TestData.UNSIGNED_MESSAGE)
//        }
//
//        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
//
//        suppose("current time is before unsigned message validity time") {
//            given(utcDateTimeProvider.getUtcDateTime())
//                .willReturn(TestData.SIGNED_MESSAGE.verifiedAt)
//        }
//
//        suppose("unsigned verification message will be deleted by ID") {
//            given(unsignedMessageRepository.deleteById(TestData.SIGNED_MESSAGE.id))
//                .willReturn(true)
//        }
//
//        val signedMessageRepository = mock<SignedVerificationMessageRepository>()
//
//        suppose("correct signed message is stored in database") {
//            given(signedMessageRepository.store(TestData.SIGNED_MESSAGE))
//                .willReturn(TestData.SIGNED_MESSAGE)
//        }
//
//        val uuidProvider = mock<UuidProvider>()
//
//        suppose("new UUID will be used for signed message") {
//            given(uuidProvider.getUuid())
//                .willReturn(TestData.SIGNED_MESSAGE.id)
//        }
//
//        val applicationProperties = ApplicationProperties().apply {
//            verification.signedMessageValidity = Duration.ofHours(1L)
//        }
//        val service = VerificationServiceImpl(
//            unsignedMessageRepository,
//            signedMessageRepository,
//            uuidProvider,
//            utcDateTimeProvider,
//            applicationProperties
//        )
//
//        verify("valid message is correctly verified") {
//            val result = service.verifyAndStoreMessageSignature(
//                TestData.SIGNED_MESSAGE.id,
//                TestData.SIGNED_MESSAGE.signature
//            )
//
//            assertThat(result).withMessage()
//                .isEqualTo(TestData.SIGNED_MESSAGE)
//        }
//    }
}
