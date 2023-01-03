package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.generated.jooq.id.ApiKeyId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.params.CreateProjectParams
import dev3.blockchainapiservice.model.result.ApiKey
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.model.result.UserIdentifier
import dev3.blockchainapiservice.repository.ApiKeyRepository
import dev3.blockchainapiservice.repository.ProjectRepository
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.Base64

@Service
class ProjectServiceImpl(
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val randomProvider: RandomProvider,
    private val projectRepository: ProjectRepository,
    private val apiKeyRepository: ApiKeyRepository
) : ProjectService {

    companion object : KLogging() {
        private const val API_KEY_BYTES = 33
        private const val API_KEY_PREFIX_LENGTH = 5
    }

    override fun createProject(userIdentifier: UserIdentifier, params: CreateProjectParams): Project {
        logger.info { "Creating project, userIdentifier: $userIdentifier, params: $params" }

        val project = Project(
            id = uuidProvider.getUuid(ProjectId),
            ownerId = userIdentifier.id,
            issuerContractAddress = params.issuerContractAddress,
            baseRedirectUrl = params.baseRedirectUrl,
            chainId = params.chainId,
            customRpcUrl = params.customRpcUrl,
            createdAt = utcDateTimeProvider.getUtcDateTime()
        )

        return projectRepository.store(project)
    }

    override fun getProjectById(userIdentifier: UserIdentifier, id: ProjectId): Project {
        logger.debug {
            "Fetching project by ID, userIdentifier: $userIdentifier, id: $id"
        }

        return projectRepository.getById(id)
            ?.takeIf { it.ownerId == userIdentifier.id }
            ?: throw ResourceNotFoundException("Project not found for ID: $id")
    }

    override fun getProjectByIssuer(
        userIdentifier: UserIdentifier,
        issuerContactAddress: ContractAddress,
        chainId: ChainId
    ): Project {
        logger.debug {
            "Fetching project by issuer, userIdentifier: $userIdentifier," +
                " issuerContractAddress: $issuerContactAddress, chainId: $chainId"
        }

        return projectRepository.getByIssuer(issuerContactAddress, chainId)
            ?.takeIf { it.ownerId == userIdentifier.id }
            ?: throw ResourceNotFoundException("Project not found for issuer contract address: $issuerContactAddress")
    }

    override fun getAllProjectsForUser(userIdentifier: UserIdentifier): List<Project> {
        logger.debug { "Fetch all projects for user, userIdentifier: $userIdentifier" }
        return projectRepository.getAllByOwnerId(userIdentifier.id)
    }

    override fun getProjectApiKeys(userIdentifier: UserIdentifier, projectId: ProjectId): List<ApiKey> {
        logger.debug { "Fetch API keys for project, userIdentifier: $userIdentifier, projectId: $projectId" }
        val project = getProjectById(userIdentifier, projectId)
        return apiKeyRepository.getAllByProjectId(project.id)
    }

    override fun createApiKey(userIdentifier: UserIdentifier, projectId: ProjectId): ApiKey {
        logger.info { "Creating API key for project, userIdentifier: $userIdentifier, projectId: $projectId" }
        val project = getProjectById(userIdentifier, projectId)
        val apiKeyBytes = randomProvider.getBytes(API_KEY_BYTES)
        val encodedApiKey = Base64.getEncoder().encodeToString(apiKeyBytes)
        val apiKey = "${encodedApiKey.take(API_KEY_PREFIX_LENGTH)}.${encodedApiKey.drop(API_KEY_PREFIX_LENGTH)}"

        return apiKeyRepository.store(
            ApiKey(
                id = uuidProvider.getUuid(ApiKeyId),
                projectId = project.id,
                apiKey = apiKey,
                createdAt = utcDateTimeProvider.getUtcDateTime()
            )
        )
    }
}
