package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.params.CreateProjectParams
import dev3.blockchainapiservice.model.result.ApiKey
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.repository.ApiKeyRepository
import dev3.blockchainapiservice.repository.ProjectRepository
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.time.OffsetDateTime
import java.util.UUID

class ProjectServiceTest : TestBase() {

    companion object {
        private val CREATED_AT = UtcDateTime(OffsetDateTime.parse("2022-01-01T00:00:00Z"))
        private const val API_KEY_BYTES = 33
    }

    @Test
    fun mustCorrectlyCreateProject() {
        val uuid = UUID.randomUUID()
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(CREATED_AT)
        }

        val params = CreateProjectParams(
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url"
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            stripeClientId = null,
            walletAddress = WalletAddress("b")
        )
        val project = Project(
            id = uuidProvider.getUuid(),
            ownerId = userIdentifier.id,
            issuerContractAddress = params.issuerContractAddress,
            baseRedirectUrl = params.baseRedirectUrl,
            chainId = params.chainId,
            customRpcUrl = params.customRpcUrl,
            createdAt = utcDateTimeProvider.getUtcDateTime()
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be stored into the database") {
            given(projectRepository.store(project))
                .willReturn(project)
        }

        val service = ProjectServiceImpl(
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = mock()
        )

        verify("project is correctly stored into the database") {
            assertThat(service.createProject(userIdentifier, params)).withMessage()
                .isEqualTo(project)
        }
    }

    @Test
    fun mustCorrectlyFetchProjectById() {
        val project = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned by ID") {
            given(projectRepository.getById(project.id))
                .willReturn(project)
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = project.ownerId,
            stripeClientId = null,
            walletAddress = WalletAddress("b")
        )

        val service = ProjectServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = mock()
        )

        verify("project is fetched from database by ID") {
            assertThat(service.getProjectById(userIdentifier, project.id)).withMessage()
                .isEqualTo(project)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenFetchingNonExistentProjectById() {
        val projectRepository = mock<ProjectRepository>()

        suppose("null will be returned for any project ID") {
            given(projectRepository.getById(any()))
                .willReturn(null)
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            stripeClientId = null,
            walletAddress = WalletAddress("b")
        )

        val service = ProjectServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getProjectById(userIdentifier, UUID.randomUUID())
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionUserIsFetchingNonOwnedProjectById() {
        val project = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned by ID") {
            given(projectRepository.getById(project.id))
                .willReturn(project)
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            stripeClientId = null,
            walletAddress = WalletAddress("b")
        )

        val service = ProjectServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getProjectById(userIdentifier, project.id)
            }
        }
    }

    @Test
    fun mustCorrectlyFetchProjectByIssuerAddress() {
        val project = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned by issuer address") {
            given(projectRepository.getByIssuer(project.issuerContractAddress, project.chainId))
                .willReturn(project)
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = project.ownerId,
            stripeClientId = null,
            walletAddress = WalletAddress("b")
        )

        val service = ProjectServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = mock()
        )

        verify("project is fetched from database by issuer address") {
            assertThat(service.getProjectByIssuer(userIdentifier, project.issuerContractAddress, project.chainId))
                .withMessage()
                .isEqualTo(project)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenFetchingNonExistentProjectByIssuerAddress() {
        val projectRepository = mock<ProjectRepository>()
        val issuerAddress = ContractAddress("dead")
        val chainId = ChainId(1337L)

        suppose("null will be returned for project issuer address") {
            given(projectRepository.getByIssuer(issuerAddress, chainId))
                .willReturn(null)
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            stripeClientId = null,
            walletAddress = WalletAddress("b")
        )

        val service = ProjectServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getProjectByIssuer(userIdentifier, issuerAddress, chainId)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionUserIsFetchingNonOwnedProjectByIssuerAddress() {
        val project = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned by issuer address") {
            given(projectRepository.getByIssuer(project.issuerContractAddress, project.chainId))
                .willReturn(project)
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            stripeClientId = null,
            walletAddress = WalletAddress("b")
        )

        val service = ProjectServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getProjectByIssuer(userIdentifier, project.issuerContractAddress, project.chainId)
            }
        }
    }

    @Test
    fun mustCorrectlyFetchAllProjectsForUser() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            stripeClientId = null,
            walletAddress = WalletAddress("b")
        )
        val projects = listOf(
            Project(
                id = UUID.randomUUID(),
                ownerId = userIdentifier.id,
                issuerContractAddress = ContractAddress("a1"),
                baseRedirectUrl = BaseUrl("base-redirect-url-1"),
                chainId = TestData.CHAIN_ID,
                customRpcUrl = "custom-rpc-url-1",
                createdAt = CREATED_AT
            ),
            Project(
                id = UUID.randomUUID(),
                ownerId = userIdentifier.id,
                issuerContractAddress = ContractAddress("a2"),
                baseRedirectUrl = BaseUrl("base-redirect-url-2"),
                chainId = TestData.CHAIN_ID,
                customRpcUrl = "custom-rpc-url-2",
                createdAt = CREATED_AT
            )
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned for user") {
            given(projectRepository.getAllByOwnerId(userIdentifier.id))
                .willReturn(projects)
        }

        val service = ProjectServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = mock()
        )

        verify("project is fetched from database by issuer address") {
            assertThat(service.getAllProjectsForUser(userIdentifier)).withMessage()
                .isEqualTo(projects)
        }
    }

    @Test
    fun mustCorrectlyFetchProjectApiKeys() {
        val project = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned by ID") {
            given(projectRepository.getById(project.id))
                .willReturn(project)
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = project.ownerId,
            stripeClientId = null,
            walletAddress = WalletAddress("b")
        )

        val apiKeys = listOf(
            ApiKey(
                id = UUID.randomUUID(),
                projectId = project.id,
                apiKey = "api-key-1",
                createdAt = CREATED_AT
            ),
            ApiKey(
                id = UUID.randomUUID(),
                projectId = project.id,
                apiKey = "api-key-2",
                createdAt = CREATED_AT
            )
        )

        val apiKeyRepository = mock<ApiKeyRepository>()

        suppose("some API keys will be returned by project ID") {
            given(apiKeyRepository.getAllByProjectId(project.id))
                .willReturn(apiKeys)
        }

        val service = ProjectServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = apiKeyRepository
        )

        verify("project is fetched from database by ID") {
            assertThat(service.getProjectApiKeys(userIdentifier, project.id)).withMessage()
                .isEqualTo(apiKeys)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUserIsFetchingProjectApiKeysForNonOwnedProject() {
        val project = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned by ID") {
            given(projectRepository.getById(project.id))
                .willReturn(project)
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            stripeClientId = null,
            walletAddress = WalletAddress("b")
        )

        val service = ProjectServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.getProjectApiKeys(userIdentifier, project.id)
            }
        }
    }

    @Test
    fun mustCorrectlyCreateApiKey() {
        val project = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned by ID") {
            given(projectRepository.getById(project.id))
                .willReturn(project)
        }

        val randomProvider = mock<RandomProvider>()

        suppose("some random bytes will be returned") {
            given(randomProvider.getBytes(API_KEY_BYTES))
                .willReturn(ByteArray(API_KEY_BYTES))
        }

        val uuid = UUID.randomUUID()
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            given(uuidProvider.getUuid())
                .willReturn(uuid)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(CREATED_AT)
        }

        val apiKey = ApiKey(
            id = uuid,
            projectId = project.id,
            apiKey = "AAAAA.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            createdAt = CREATED_AT
        )
        val apiKeyRepository = mock<ApiKeyRepository>()

        suppose("API key will be stored into the database") {
            given(apiKeyRepository.store(apiKey))
                .willReturn(apiKey)
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = project.ownerId,
            stripeClientId = null,
            walletAddress = WalletAddress("b")
        )

        val service = ProjectServiceImpl(
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            randomProvider = randomProvider,
            projectRepository = projectRepository,
            apiKeyRepository = apiKeyRepository
        )

        verify("API key is correctly stored into the database") {
            assertThat(service.createApiKey(userIdentifier, project.id)).withMessage()
                .isEqualTo(apiKey)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUserCreatesApiKeyForNonOwnedProject() {
        val project = Project(
            id = UUID.randomUUID(),
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned by ID") {
            given(projectRepository.getById(project.id))
                .willReturn(project)
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            stripeClientId = null,
            walletAddress = WalletAddress("b")
        )

        val service = ProjectServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            assertThrows<ResourceNotFoundException>(message) {
                service.createApiKey(userIdentifier, project.id)
            }
        }
    }
}
