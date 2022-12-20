package dev3.blockchainapiservice.features.promo_code.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.features.promocodes.controller.PromoCodeController
import dev3.blockchainapiservice.features.promocodes.model.request.GeneratePromoCodeRequest
import dev3.blockchainapiservice.features.promocodes.model.response.PromoCodeResponse
import dev3.blockchainapiservice.features.promocodes.model.response.PromoCodesResponse
import dev3.blockchainapiservice.features.promocodes.model.result.PromoCode
import dev3.blockchainapiservice.features.promocodes.service.PromoCodeService
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.service.UtcDateTimeProvider
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration
import org.mockito.kotlin.verify as verifyMock

class PromoCodeControllerTest : TestBase() {

    companion object {
        private val USER_IDENTIFIER = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            stripeClientId = null,
            walletAddress = WalletAddress("abc")
        )
    }

    @Test
    fun mustCorrectlyGetPromoCodes() {
        val result = PromoCodesResponse(
            listOf(
                PromoCodeResponse(
                    code = "code",
                    writeRequests = BigInteger.ONE,
                    readRequests = BigInteger.TWO,
                    numOfUsages = BigInteger.ZERO,
                    validUntil = TestData.TIMESTAMP.value
                )
            )
        )

        val service = mock<PromoCodeService>()
        val from = TestData.TIMESTAMP
        val to = TestData.TIMESTAMP + 1.days

        suppose("some promo codes will be returned") {
            given(service.getPromoCodes(USER_IDENTIFIER, from, to))
                .willReturn(
                    result.promoCodes.map {
                        PromoCode(
                            code = it.code,
                            writeRequests = it.writeRequests.longValueExact(),
                            readRequests = it.readRequests.longValueExact(),
                            numOfUsages = it.numOfUsages.longValueExact(),
                            validUntil = UtcDateTime(it.validUntil)
                        )
                    }
                )
        }

        val controller = PromoCodeController(service, mock())

        verify("controller returns correct response") {
            val response = controller.getPromoCodes(USER_IDENTIFIER, from.value, to.value)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(result))
        }
    }

    @Test
    fun mustCorrectlyGeneratePromoCode() {
        val result = PromoCodeResponse(
            code = "DEV3-ABCDEF",
            writeRequests = BigInteger.ONE,
            readRequests = BigInteger.TWO,
            numOfUsages = BigInteger.ZERO,
            validUntil = TestData.TIMESTAMP.value + 11.days.toJavaDuration()
        )

        val service = mock<PromoCodeService>()

        suppose("some promo code will be generated") {
            given(
                service.generatePromoCode(
                    userIdentifier = USER_IDENTIFIER,
                    prefix = "DEV3-",
                    writeRequests = result.writeRequests.longValueExact(),
                    readRequests = result.readRequests.longValueExact(),
                    validUntil = TestData.TIMESTAMP + 11.days
                )
            )
                .willReturn(
                    PromoCode(
                        code = result.code,
                        writeRequests = result.writeRequests.longValueExact(),
                        readRequests = result.readRequests.longValueExact(),
                        numOfUsages = result.numOfUsages.longValueExact(),
                        validUntil = TestData.TIMESTAMP + 11.days
                    )
                )
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val controller = PromoCodeController(service, utcDateTimeProvider)

        verify("controller returns correct response") {
            val request = GeneratePromoCodeRequest(
                prefix = "DEV3-",
                writeRequests = result.writeRequests.longValueExact(),
                readRequests = result.readRequests.longValueExact(),
                validityInDays = 11L
            )
            val response = controller.generatePromoCode(USER_IDENTIFIER, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(result))
        }
    }

    @Test
    fun mustCorrectlyUsePromoCode() {
        val service = mock<PromoCodeService>()
        val controller = PromoCodeController(service, mock())

        verify("promo code is correctly used") {
            controller.usePromoCode(USER_IDENTIFIER, "code")

            verifyMock(service)
                .usePromoCode(USER_IDENTIFIER, "code")
            verifyNoMoreInteractions(service)
        }
    }
}
