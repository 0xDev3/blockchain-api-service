package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.model.response.ApiUsagePeriodResponse
import dev3.blockchainapiservice.model.result.ApiUsagePeriod
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.model.result.RequestUsage
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.service.UtcDateTimeProvider
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.time.Duration
import java.util.UUID

class ApiUsageControllerTest : TestBase() {

    companion object {
        private val USER_IDENTIFIER = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            stripeClientId = null,
            walletAddress = WalletAddress("a")
        )
        private val PROJECT = Project(
            id = UUID.randomUUID(),
            ownerId = USER_IDENTIFIER.id,
            issuerContractAddress = ContractAddress("155034"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        private val API_USAGE_PERIOD = ApiUsagePeriod(
            userId = USER_IDENTIFIER.id,
            writeRequestUsage = RequestUsage(0, 0),
            readRequestUsage = RequestUsage(1, 1),
            startDate = TestData.TIMESTAMP,
            endDate = TestData.TIMESTAMP + Duration.ofDays(30L)
        )
        private val RESPONSE = ResponseEntity.ok(
            ApiUsagePeriodResponse(
                userId = USER_IDENTIFIER.id,
                writeRequestUsage = RequestUsage(0, 0),
                readRequestUsage = RequestUsage(1, 1),
                startDate = TestData.TIMESTAMP.value,
                endDate = (TestData.TIMESTAMP + Duration.ofDays(30L)).value
            )
        )
    }

    @Test
    fun mustCorrectlyGetCurrentApiUsageInfoForUser() {
        val apiRateLimitRepository = mock<ApiRateLimitRepository>()

        suppose("some api usage will be returned") {
            given(apiRateLimitRepository.getCurrentApiUsagePeriod(USER_IDENTIFIER.id, TestData.TIMESTAMP))
                .willReturn(API_USAGE_PERIOD)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val controller = ApiUsageController(apiRateLimitRepository, utcDateTimeProvider)

        verify("controller returns correct response") {
            val response = controller.getCurrentApiUsageInfoForUser(USER_IDENTIFIER)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(RESPONSE)
        }
    }

    @Test
    fun mustCorrectlyGetCurrentApiUsageInfoForApiKey() {
        val apiRateLimitRepository = mock<ApiRateLimitRepository>()

        suppose("some api usage will be returned") {
            given(apiRateLimitRepository.getCurrentApiUsagePeriod(USER_IDENTIFIER.id, TestData.TIMESTAMP))
                .willReturn(API_USAGE_PERIOD)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val controller = ApiUsageController(apiRateLimitRepository, utcDateTimeProvider)

        verify("controller returns correct response") {
            val response = controller.getCurrentApiUsageInfoForApiKey(PROJECT)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(RESPONSE)
        }
    }
}
