package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.exception.ApiKeyAlreadyExistsException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.generated.jooq.id.ApiKeyId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.model.params.CreateProjectParams
import dev3.blockchainapiservice.model.request.CreateProjectRequest
import dev3.blockchainapiservice.model.response.ApiKeyResponse
import dev3.blockchainapiservice.model.response.ProjectResponse
import dev3.blockchainapiservice.model.response.ProjectsResponse
import dev3.blockchainapiservice.model.result.ApiKey
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.service.AnalyticsService
import dev3.blockchainapiservice.service.ProjectService
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import java.time.OffsetDateTime
import java.util.UUID

class ProjectControllerTest : TestBase() {

    companion object {
        private val CREATED_AT = UtcDateTime(OffsetDateTime.parse("2022-01-01T00:00:00Z"))
    }

    @Test
    fun mustCorrectlyCreateProject() {
        val params = CreateProjectParams(
            issuerContractAddress = ContractAddress("155034"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url"
        )
        val result = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            issuerContractAddress = params.issuerContractAddress,
            baseRedirectUrl = params.baseRedirectUrl,
            chainId = params.chainId,
            customRpcUrl = params.customRpcUrl,
            createdAt = CREATED_AT
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = result.ownerId,
            stripeClientId = null,
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()
        val analyticsService = mock<AnalyticsService>()

        suppose("project will be created") {
            call(service.createProject(userIdentifier, params))
                .willReturn(result)
        }

        val controller = ProjectController(service, analyticsService)

        verify("controller returns correct response") {
            val request = CreateProjectRequest(
                issuerContractAddress = params.issuerContractAddress.rawValue,
                baseRedirectUrl = params.baseRedirectUrl.value,
                chainId = params.chainId.value,
                customRpcUrl = params.customRpcUrl
            )
            val response = controller.createProject(userIdentifier, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(ProjectResponse(result)))
        }
    }

    @Test
    fun mustCorrectlyGetProjectById() {
        val result = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            issuerContractAddress = ContractAddress("155034"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = result.ownerId,
            stripeClientId = null,
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()
        val analyticsService = mock<AnalyticsService>()

        suppose("project will be returned") {
            call(service.getProjectById(userIdentifier, result.id))
                .willReturn(result)
        }

        val controller = ProjectController(service, analyticsService)

        verify("controller returns correct response") {
            val response = controller.getById(userIdentifier, result.id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(ProjectResponse(result)))
        }
    }

    @Test
    fun mustCorrectlyGetProjectByIssuerAddress() {
        val result = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            issuerContractAddress = ContractAddress("155034"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = result.ownerId,
            stripeClientId = null,
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()
        val analyticsService = mock<AnalyticsService>()

        suppose("project will be returned") {
            call(service.getProjectByIssuer(userIdentifier, result.issuerContractAddress, result.chainId))
                .willReturn(result)
        }

        val controller = ProjectController(service, analyticsService)

        verify("controller returns correct response") {
            val response = controller.getByIssuerAddress(
                userIdentifier = userIdentifier,
                chainId = result.chainId.value,
                issuerAddress = result.issuerContractAddress.rawValue
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(ProjectResponse(result)))
        }
    }

    @Test
    fun mustCorrectlyGetAllProjectsForUser() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            stripeClientId = null,
            walletAddress = WalletAddress("a")
        )
        val result = listOf(
            Project(
                id = ProjectId(UUID.randomUUID()),
                ownerId = userIdentifier.id,
                issuerContractAddress = ContractAddress("155034a"),
                baseRedirectUrl = BaseUrl("base-redirect-url-1"),
                chainId = TestData.CHAIN_ID,
                customRpcUrl = "custom-rpc-url-1",
                createdAt = CREATED_AT
            ),
            Project(
                id = ProjectId(UUID.randomUUID()),
                ownerId = userIdentifier.id,
                issuerContractAddress = ContractAddress("155034b"),
                baseRedirectUrl = BaseUrl("base-redirect-url-2"),
                chainId = TestData.CHAIN_ID,
                customRpcUrl = "custom-rpc-url-2",
                createdAt = CREATED_AT
            )
        )

        val service = mock<ProjectService>()
        val analyticsService = mock<AnalyticsService>()

        suppose("projects will be returned") {
            call(service.getAllProjectsForUser(userIdentifier))
                .willReturn(result)
        }

        val controller = ProjectController(service, analyticsService)

        verify("controller returns correct response") {
            val response = controller.getAll(userIdentifier)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(ProjectsResponse(result.map { ProjectResponse(it) })))
        }
    }

    @Test
    fun mustCorrectlyGetApiKey() {
        val result = ApiKey(
            id = ApiKeyId(UUID.randomUUID()),
            projectId = ProjectId(UUID.randomUUID()),
            apiKey = "api-key",
            createdAt = CREATED_AT
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            stripeClientId = null,
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()
        val analyticsService = mock<AnalyticsService>()

        suppose("API key will be returned") {
            call(service.getProjectApiKeys(userIdentifier, result.projectId))
                .willReturn(listOf(result))
        }

        val controller = ProjectController(service, analyticsService)

        verify("controller returns correct response") {
            val response = controller.getApiKey(userIdentifier, result.projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(ApiKeyResponse(result)))
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentApiKey() {
        val projectId = ProjectId(UUID.randomUUID())
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            stripeClientId = null,
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()
        val analyticsService = mock<AnalyticsService>()

        suppose("empty list will be returned") {
            call(service.getProjectApiKeys(userIdentifier, projectId))
                .willReturn(listOf())
        }

        val controller = ProjectController(service, analyticsService)

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                controller.getApiKey(userIdentifier, projectId)
            }
        }
    }

    @Test
    fun mustCorrectlyCreateApiKey() {
        val result = ApiKey(
            id = ApiKeyId(UUID.randomUUID()),
            projectId = ProjectId(UUID.randomUUID()),
            apiKey = "api-key",
            createdAt = CREATED_AT
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            stripeClientId = null,
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()
        val analyticsService = mock<AnalyticsService>()

        suppose("no API keys exist") {
            call(service.getProjectApiKeys(userIdentifier, result.projectId))
                .willReturn(emptyList())
        }

        suppose("API key will be created") {
            call(service.createApiKey(userIdentifier, result.projectId))
                .willReturn(result)
        }

        val controller = ProjectController(service, analyticsService)

        verify("controller returns correct response") {
            val response = controller.createApiKey(userIdentifier, result.projectId, MockHttpServletRequest())

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(ApiKeyResponse(result)))
        }
    }

    @Test
    fun mustThrowApiKeyAlreadyExistsExceptionWhenApiKeyAlreadyExists() {
        val result = ApiKey(
            id = ApiKeyId(UUID.randomUUID()),
            projectId = ProjectId(UUID.randomUUID()),
            apiKey = "api-key",
            createdAt = CREATED_AT
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            stripeClientId = null,
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()
        val analyticsService = mock<AnalyticsService>()

        suppose("single API keys exist") {
            call(service.getProjectApiKeys(userIdentifier, result.projectId))
                .willReturn(listOf(result))
        }

        val controller = ProjectController(service, analyticsService)

        verify("ApiKeyAlreadyExistsException is thrown") {
            expectThrows<ApiKeyAlreadyExistsException> {
                controller.createApiKey(userIdentifier, result.projectId, MockHttpServletRequest())
            }
        }
    }
}
