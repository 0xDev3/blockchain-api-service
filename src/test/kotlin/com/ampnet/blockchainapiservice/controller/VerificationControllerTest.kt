package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.model.response.GenerateVerificationMessageResponse
import com.ampnet.blockchainapiservice.model.result.UnsignedVerificationMessage
import com.ampnet.blockchainapiservice.service.VerificationService
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

class VerificationControllerTest : TestBase() {

    @Test
    fun mustReturnGeneratedVerificationMessage() {
        val walletAddress = WalletAddress("123")
        val validityDuration = Duration.ofDays(1L)

        val message = UnsignedVerificationMessage(
            id = UUID.randomUUID(),
            walletAddress = walletAddress,
            createdAt = UtcDateTime(OffsetDateTime.parse("2022-01-01T00:00:00Z")),
            validityDuration = validityDuration
        )
        val service = mock<VerificationService>()

        suppose("service will return some unsigned verification message") {
            given(service.createUnsignedVerificationMessage(walletAddress))
                .willReturn(message)
        }

        val controller = VerificationController(service)

        verify("controller returns correct response") {
            val result = controller.generateVerificationMessage(walletAddress.rawValue)

            assertThat(result).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        GenerateVerificationMessageResponse(
                            id = message.id,
                            message = message.toStringMessage(),
                            validUntil = message.validUntil.value
                        )
                    )
                )
        }
    }
}
