package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.JsonSchemaDocumentation
import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.exception.ApiKeyAlreadyExistsException
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.params.CreateProjectParams
import com.ampnet.blockchainapiservice.model.request.CreateProjectRequest
import com.ampnet.blockchainapiservice.model.response.ApiKeyResponse
import com.ampnet.blockchainapiservice.model.response.ProjectResponse
import com.ampnet.blockchainapiservice.model.response.ProjectsResponse
import com.ampnet.blockchainapiservice.model.result.ApiKey
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.model.result.UserWalletAddressIdentifier
import com.ampnet.blockchainapiservice.service.ProjectService
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
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
            redirectUrl = "redirect-url",
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = "custom-rpc-url"
        )
        val result = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = params.issuerContractAddress,
            redirectUrl = params.redirectUrl,
            chainId = params.chainId,
            customRpcUrl = params.customRpcUrl,
            createdAt = CREATED_AT
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = result.ownerId,
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()

        suppose("project will be created") {
            given(service.createProject(userIdentifier, params))
                .willReturn(result)
        }

        val controller = ProjectController(service)

        verify("controller returns correct response") {
            val request = CreateProjectRequest(
                issuerContractAddress = params.issuerContractAddress.rawValue,
                redirectUrl = params.redirectUrl,
                chainId = params.chainId.value,
                customRpcUrl = params.customRpcUrl
            )
            val response = controller.createProject(userIdentifier, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(ProjectResponse(result)))
        }
    }

    @Test
    fun mustCorrectlyGetProjectById() {
        val result = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("155034"),
            redirectUrl = "redirect-url",
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = result.ownerId,
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()

        suppose("project will be returned") {
            given(service.getProjectById(userIdentifier, result.id))
                .willReturn(result)
        }

        val controller = ProjectController(service)

        verify("controller returns correct response") {
            val response = controller.getById(userIdentifier, result.id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(ProjectResponse(result)))
        }
    }

    @Test
    fun mustCorrectlyGetProjectByIssuerAddress() {
        val result = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("155034"),
            redirectUrl = "redirect-url",
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = result.ownerId,
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()

        suppose("project will be returned") {
            given(service.getProjectByIssuerAddress(userIdentifier, result.issuerContractAddress))
                .willReturn(result)
        }

        val controller = ProjectController(service)

        verify("controller returns correct response") {
            val response = controller.getByIssuerAddress(userIdentifier, result.issuerContractAddress.rawValue)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(ProjectResponse(result)))
        }
    }

    @Test
    fun mustCorrectlyGetAllProjectsForUser() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress("a")
        )
        val result = listOf(
            Project(
                id = UUID.randomUUID(),
                ownerId = userIdentifier.id,
                issuerContractAddress = ContractAddress("155034a"),
                redirectUrl = "redirect-url-1",
                chainId = Chain.HARDHAT_TESTNET.id,
                customRpcUrl = "custom-rpc-url-1",
                createdAt = CREATED_AT
            ),
            Project(
                id = UUID.randomUUID(),
                ownerId = userIdentifier.id,
                issuerContractAddress = ContractAddress("155034b"),
                redirectUrl = "redirect-url-2",
                chainId = Chain.HARDHAT_TESTNET.id,
                customRpcUrl = "custom-rpc-url-2",
                createdAt = CREATED_AT
            )
        )

        val service = mock<ProjectService>()

        suppose("projects will be returned") {
            given(service.getAllProjectsForUser(userIdentifier))
                .willReturn(result)
        }

        val controller = ProjectController(service)

        verify("controller returns correct response") {
            val response = controller.getAll(userIdentifier)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(ProjectsResponse(result.map { ProjectResponse(it) })))
        }
    }

    @Test
    fun mustCorrectlyGetApiKey() {
        val result = ApiKey(
            id = UUID.randomUUID(),
            projectId = UUID.randomUUID(),
            apiKey = "api-key",
            createdAt = CREATED_AT
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()

        suppose("API key will be returned") {
            given(service.getProjectApiKeys(userIdentifier, result.projectId))
                .willReturn(listOf(result))
        }

        val controller = ProjectController(service)

        verify("controller returns correct response") {
            val response = controller.getApiKey(userIdentifier, result.projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(ApiKeyResponse(result)))
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentApiKey() {
        val projectId = UUID.randomUUID()
        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()

        suppose("empty list will be returned") {
            given(service.getProjectApiKeys(userIdentifier, projectId))
                .willReturn(listOf())
        }

        val controller = ProjectController(service)

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                controller.getApiKey(userIdentifier, projectId)
            }
        }
    }

    @Test
    fun mustCorrectlyCreateApiKey() {
        val result = ApiKey(
            id = UUID.randomUUID(),
            projectId = UUID.randomUUID(),
            apiKey = "api-key",
            createdAt = CREATED_AT
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()

        suppose("no API keys exist") {
            given(service.getProjectApiKeys(userIdentifier, result.projectId))
                .willReturn(emptyList())
        }

        suppose("API key will be created") {
            given(service.createApiKey(userIdentifier, result.projectId))
                .willReturn(result)
        }

        val controller = ProjectController(service)

        verify("controller returns correct response") {
            val response = controller.createApiKey(userIdentifier, result.projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(ApiKeyResponse(result)))
        }
    }

    @Test
    fun mustThrowApiKeyAlreadyExistsExceptionWhenApiKeyAlreadyExists() {
        val result = ApiKey(
            id = UUID.randomUUID(),
            projectId = UUID.randomUUID(),
            apiKey = "api-key",
            createdAt = CREATED_AT
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()

        suppose("single API keys exist") {
            given(service.getProjectApiKeys(userIdentifier, result.projectId))
                .willReturn(listOf(result))
        }

        val controller = ProjectController(service)

        verify("ApiKeyAlreadyExistsException is thrown") {
            assertThrows<ApiKeyAlreadyExistsException>(message) {
                controller.createApiKey(userIdentifier, result.projectId)
            }
        }
    }
}
