package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.ControllerTestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.exception.ErrorCode
import com.ampnet.blockchainapiservice.model.response.GenerateVerificationMessageResponse
import com.ampnet.blockchainapiservice.model.response.VerifySignedMessageResponse
import com.ampnet.blockchainapiservice.model.result.UnsignedVerificationMessage
import com.ampnet.blockchainapiservice.repository.SignedVerificationMessageRepository
import com.ampnet.blockchainapiservice.repository.UnsignedVerificationMessageRepository
import com.ampnet.blockchainapiservice.service.UtcDateTimeProvider
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.Duration
import com.ampnet.blockchainapiservice.generated.jooq.tables.SignedVerificationMessage as SignedVerificationMessageTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.UnsignedVerificationMessage as UnsignedVerificationMessageTable

class VerificationControllerApiTest : ControllerTestBase() {

    @Autowired
    private lateinit var unsignedVerificationMessageRepository: UnsignedVerificationMessageRepository

    @Autowired
    private lateinit var signedVerificationMessageRepository: SignedVerificationMessageRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @MockBean
    private lateinit var utcDateTimeProvider: UtcDateTimeProvider

    @BeforeEach
    fun beforeEach() {
        dslContext.deleteFrom(UnsignedVerificationMessageTable.UNSIGNED_VERIFICATION_MESSAGE).execute()
        dslContext.deleteFrom(SignedVerificationMessageTable.SIGNED_VERIFICATION_MESSAGE).execute()
    }

    @Test
    fun mustCorrectlyGenerateVerificationMessageAndStoreItInDatabase() {
        suppose("some fixed date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.UNSIGNED_MESSAGE.createdAt)
        }

        val generatedMessage = suppose("request to generate verification message is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/verification/generate/${walletAddress.rawValue}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, GenerateVerificationMessageResponse::class.java)
        }

        val databaseMessage = unsignedVerificationMessageRepository.getById(generatedMessage.id)

        verify("generated verification message is present in database") {
            assertThat(databaseMessage).withMessage()
                .isNotNull()
                .isEqualTo(
                    UnsignedVerificationMessage(
                        id = databaseMessage!!.id,
                        walletAddress = walletAddress,
                        createdAt = databaseMessage.createdAt,
                        validUntil = databaseMessage.validUntil
                    )
                )
        }

        verify("generated response message has correct payload") {
            assertThat(generatedMessage).withMessage()
                .isEqualTo(
                    GenerateVerificationMessageResponse(
                        id = databaseMessage!!.id,
                        message = databaseMessage.toStringMessage(),
                        validUntil = databaseMessage.validUntil.value
                    )
                )
        }
    }

    @Test
    fun mustReturn404NotFoundWhenVerifyingNonExistentMessage() {
        suppose("some fixed date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.UNSIGNED_MESSAGE.createdAt)
        }

        verify("404 is returned for non-existent message") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/verification/signature/${TestData.UNSIGNED_MESSAGE.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\n    \"signature\":\"${TestData.SIGNED_MESSAGE.signature}\"\n}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustReturn412PreconditionFailedWhenVerifyingExpiredMessage() {
        suppose("some fixed date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.UNSIGNED_MESSAGE.createdAt)
        }

        suppose("expired unsigned verification message exists in database") {
            unsignedVerificationMessageRepository.store(
                TestData.UNSIGNED_MESSAGE.copy(
                    validUntil = TestData.UNSIGNED_MESSAGE.createdAt - Duration.ofMinutes(1L)
                )
            )
        }

        verify("412 is returned for expired message") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/verification/signature/${TestData.UNSIGNED_MESSAGE.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\n    \"signature\":\"${TestData.SIGNED_MESSAGE.signature}\"\n}")
            )
                .andExpect(MockMvcResultMatchers.status().isPreconditionFailed)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.VALIDATION_MESSAGE_EXPIRED)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenVerifyingSignatureWithInvalidLength() {
        suppose("some fixed date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.UNSIGNED_MESSAGE.createdAt)
        }

        suppose("non-expired unsigned verification message exists in database") {
            unsignedVerificationMessageRepository.store(TestData.UNSIGNED_MESSAGE)
        }

        verify("400 is returned for invalid signature length") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/verification/signature/${TestData.UNSIGNED_MESSAGE.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\n    \"signature\":\"${TestData.TOO_SHORT_SIGNATURE}\"\n}")
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.BAD_SIGNATURE)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenVerifyingSignatureWithValidLengthAndInvalidFormat() {
        suppose("some fixed date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.UNSIGNED_MESSAGE.createdAt)
        }

        suppose("non-expired unsigned verification message exists in database") {
            unsignedVerificationMessageRepository.store(TestData.UNSIGNED_MESSAGE)
        }

        verify("400 is returned for invalid signature format") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/verification/signature/${TestData.UNSIGNED_MESSAGE.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\n    \"signature\":\"${TestData.INVALID_SIGNATURE}\"\n}")
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.BAD_SIGNATURE)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenMessageSignatureIsInvalid() {
        suppose("some fixed date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.UNSIGNED_MESSAGE.createdAt)
        }

        suppose("non-expired unsigned verification message exists in database") {
            unsignedVerificationMessageRepository.store(TestData.UNSIGNED_MESSAGE)
        }

        verify("400 is returned for signature mismatch") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/verification/signature/${TestData.UNSIGNED_MESSAGE.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\n    \"signature\":\"${TestData.OTHER_SIGNATURE}\"\n}")
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.BAD_SIGNATURE)
        }
    }

    @Test
    fun mustCorrectlyVerifyValidMessageSignature() {
        suppose("some fixed date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.UNSIGNED_MESSAGE.createdAt)
        }

        suppose("non-expired unsigned verification message exists in database") {
            unsignedVerificationMessageRepository.store(TestData.UNSIGNED_MESSAGE)
        }

        val signedMessage = suppose("request to verify message signature is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/verification/signature/${TestData.UNSIGNED_MESSAGE.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\n    \"signature\":\"${TestData.SIGNED_MESSAGE.signature}\"\n}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, VerifySignedMessageResponse::class.java)
        }

        verify("unsigned verification message is deleted from database") {
            val result = unsignedVerificationMessageRepository.getById(TestData.UNSIGNED_MESSAGE.id)

            assertThat(result).withMessage()
                .isNull()
        }

        val signedMessageValidity = ApplicationProperties().verification.signedMessageValidity

        verify("signed verification message is stored in database") {
            val result = signedVerificationMessageRepository.getById(TestData.SIGNED_MESSAGE.id)

            assertThat(result).withMessage()
                .isEqualTo(
                    TestData.SIGNED_MESSAGE.copy(
                        verifiedAt = utcDateTimeProvider.getUtcDateTime(),
                        validUntil = utcDateTimeProvider.getUtcDateTime() + signedMessageValidity
                    )
                )
        }

        verify("correct response is returned") {
            assertThat(signedMessage).withMessage()
                .isEqualTo(
                    VerifySignedMessageResponse(
                        id = TestData.SIGNED_MESSAGE.id,
                        signature = TestData.SIGNED_MESSAGE.signature,
                        validUntil = utcDateTimeProvider.getUtcDateTime().value + signedMessageValidity
                    )
                )
        }
    }
}
