package dev3.blockchainapiservice.features.payout.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.config.ApplicationProperties
import dev3.blockchainapiservice.config.WebConfig
import dev3.blockchainapiservice.exception.IpfsUploadFailedException
import dev3.blockchainapiservice.features.payout.util.IpfsHash
import dev3.blockchainapiservice.repository.ApiKeyRepository
import dev3.blockchainapiservice.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.repository.ProjectRepository
import dev3.blockchainapiservice.repository.UserIdResolverRepository
import dev3.blockchainapiservice.repository.UserIdentifierRepository
import dev3.blockchainapiservice.service.FunctionEncoderService
import dev3.blockchainapiservice.service.RandomUuidProvider
import dev3.blockchainapiservice.service.UtcDateTimeProvider
import dev3.blockchainapiservice.wiremock.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType

@RestClientTest
@Import(PinataIpfsService::class, ApplicationProperties::class, WebConfig::class)
@MockBeans(
    MockBean(RandomUuidProvider::class),
    MockBean(UserIdentifierRepository::class),
    MockBean(ApiKeyRepository::class),
    MockBean(ProjectRepository::class),
    MockBean(BlockchainService::class),
    MockBean(FunctionEncoderService::class),
    MockBean(UtcDateTimeProvider::class),
    MockBean(ApiRateLimitRepository::class),
    MockBean(UserIdResolverRepository::class)
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PinataIpfsServiceIntegTest : TestBase() {

    @Autowired
    private lateinit var service: IpfsService

    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun beforeEach() {
        WireMock.start()
    }

    @AfterEach
    fun afterEach() {
        WireMock.stop()
    }

    @Test
    fun mustCorrectlyUploadJsonToIpfs() {
        val requestJson = "{\"test\":1}"
        val ipfsHash = IpfsHash("test-hash")
        val responseJson =
            """
            {
                "IpfsHash": "${ipfsHash.value}",
                "PinSize": 1,
                "Timestamp": "2022-01-01T00:00:00Z"
            }
            """.trimIndent()

        suppose("IPFS JSON upload will succeed") {
            WireMock.server.stubFor(
                post(urlPathEqualTo("/pinning/pinJSONToIPFS"))
                    .withHeader("pinata_api_key", equalTo("test-api-key"))
                    .withHeader("pinata_secret_api_key", equalTo("test-api-secret"))
                    .withRequestBody(equalToJson(requestJson))
                    .willReturn(
                        aResponse()
                            .withBody(responseJson)
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withStatus(200)
                    )
            )
        }

        verify("correct IPFS hash is returned for JSON upload") {
            val result = service.pinJsonToIpfs(objectMapper.readTree(requestJson))

            assertThat(result).withMessage()
                .isEqualTo(ipfsHash)
        }
    }

    @Test
    fun mustThrowExceptionWhenIpfsHashIsMissingInResponse() {
        val requestJson = "{\"test\":1}"
        val responseJson =
            """
            {
                "PinSize": 1,
                "Timestamp": "2022-01-01T00:00:00Z"
            }
            """.trimIndent()

        suppose("IPFS JSON upload will succeed without IPFS hash in response") {
            WireMock.server.stubFor(
                post(urlPathEqualTo("/pinning/pinJSONToIPFS"))
                    .withHeader("pinata_api_key", equalTo("test-api-key"))
                    .withHeader("pinata_secret_api_key", equalTo("test-api-secret"))
                    .withRequestBody(equalToJson(requestJson))
                    .willReturn(
                        aResponse()
                            .withBody(responseJson)
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withStatus(200)
                    )
            )
        }

        verify("exception is thrown when IPFS hash is missing in response") {
            assertThrows<IpfsUploadFailedException>(message) {
                service.pinJsonToIpfs(objectMapper.readTree(requestJson))
            }
        }
    }

    @Test
    fun mustThrowExceptionForNon2xxResponseCode() {
        val requestJson = "{\"test\":1}"

        suppose("IPFS JSON upload will succeed without IPFS hash in response") {
            WireMock.server.stubFor(
                post(urlPathEqualTo("/pinning/pinJSONToIPFS"))
                    .withHeader("pinata_api_key", equalTo("test-api-key"))
                    .withHeader("pinata_secret_api_key", equalTo("test-api-secret"))
                    .withRequestBody(equalToJson(requestJson))
                    .willReturn(
                        aResponse()
                            .withBody("{}")
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withStatus(400)
                    )
            )
        }

        verify("exception is thrown when non 2xx response is returned") {
            assertThrows<IpfsUploadFailedException>(message) {
                service.pinJsonToIpfs(objectMapper.readTree(requestJson))
            }
        }
    }
}
