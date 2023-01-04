package dev3.blockchainapiservice.features.promocode.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.features.api.access.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.features.api.promocodes.controller.PromoCodeController
import dev3.blockchainapiservice.features.api.promocodes.model.request.GeneratePromoCodeRequest
import dev3.blockchainapiservice.features.api.promocodes.model.response.PromoCodeResponse
import dev3.blockchainapiservice.features.api.promocodes.model.response.PromoCodesResponse
import dev3.blockchainapiservice.features.api.promocodes.model.result.PromoCode
import dev3.blockchainapiservice.features.api.promocodes.service.PromoCodeService
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.service.UtcDateTimeProvider
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

class PromoCodeControllerTest : TestBase() {

    companion object {
        private val USER_IDENTIFIER = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
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
            call(service.getPromoCodes(USER_IDENTIFIER, from, to))
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

            expectThat(response)
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
            call(
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
            call(utcDateTimeProvider.getUtcDateTime())
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

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(result))
        }
    }

    @Test
    fun mustCorrectlyUsePromoCode() {
        val service = mock<PromoCodeService>()
        val controller = PromoCodeController(service, mock())

        verify("promo code is correctly used") {
            controller.usePromoCode(USER_IDENTIFIER, "code")

            expectInteractions(service) {
                once.usePromoCode(USER_IDENTIFIER, "code")
            }
        }
    }
}
