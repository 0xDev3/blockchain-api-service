package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.params.CreateProjectParams
import com.ampnet.blockchainapiservice.model.result.ApiKey
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.model.result.UserIdentifier
import com.ampnet.blockchainapiservice.repository.ApiKeyRepository
import com.ampnet.blockchainapiservice.repository.ProjectRepository
import com.ampnet.blockchainapiservice.util.ContractAddress
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.UUID

@Service
class ProjectServiceImpl(
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val randomProvider: RandomProvider,
    private val projectRepository: ProjectRepository,
    private val apiKeyRepository: ApiKeyRepository
) : ProjectService {

    companion object : KLogging() {
        private const val API_KEY_BYTES = 66
        private const val API_KEY_PREFIX_LENGTH = 6
    }

    override fun createProject(userIdentifier: UserIdentifier, params: CreateProjectParams): Project {
        logger.info { "Creating project, userIdentifier: $userIdentifier, params: $params" }

        val project = Project(
            id = uuidProvider.getUuid(),
            ownerId = userIdentifier.id,
            issuerContractAddress = params.issuerContractAddress,
            redirectUrl = params.redirectUrl,
            chainId = params.chainId,
            customRpcUrl = params.customRpcUrl,
            createdAt = utcDateTimeProvider.getUtcDateTime()
        )

        return projectRepository.store(project)
    }

    override fun getProjectById(userIdentifier: UserIdentifier, id: UUID): Project {
        logger.debug {
            "Fetching project by ID, userIdentifier: $userIdentifier, id: $id"
        }

        return projectRepository.getById(id)
            ?.takeIf { it.ownerId == userIdentifier.id }
            ?: throw ResourceNotFoundException("Project not found for ID: $id")
    }

    override fun getProjectByIssuerAddress(
        userIdentifier: UserIdentifier,
        issuerContactAddress: ContractAddress
    ): Project {
        logger.debug {
            "Fetching project by issuer, userIdentifier: $userIdentifier, issuerContractAddress: $issuerContactAddress"
        }

        return projectRepository.getByIssuerContractAddress(issuerContactAddress)
            ?.takeIf { it.ownerId == userIdentifier.id }
            ?: throw ResourceNotFoundException("Project not found for issuer contract address: $issuerContactAddress")
    }

    override fun getAllProjectsForUser(userIdentifier: UserIdentifier): List<Project> {
        logger.debug { "Fetch all projects for user, userIdentifier: $userIdentifier" }
        return projectRepository.getAllByOwnerId(userIdentifier.id)
    }

    override fun getProjectApiKeys(userIdentifier: UserIdentifier, projectId: UUID): List<ApiKey> {
        logger.debug { "Fetch API keys for project, userIdentifier: $userIdentifier, projectId: $projectId" }
        val project = getProjectById(userIdentifier, projectId)
        return apiKeyRepository.getAllByProjectId(project.id)
    }

    override fun createApiKey(userIdentifier: UserIdentifier, projectId: UUID): ApiKey {
        logger.info { "Creating API key for project, userIdentifier: $userIdentifier, projectId: $projectId" }
        val project = getProjectById(userIdentifier, projectId)
        val apiKeyBytes = randomProvider.getBytes(API_KEY_BYTES)
        val encodedApiKey = Base64.getEncoder().encodeToString(apiKeyBytes)
        val apiKey = "${encodedApiKey.take(API_KEY_PREFIX_LENGTH)}.${encodedApiKey.drop(API_KEY_PREFIX_LENGTH)}"

        return apiKeyRepository.store(
            ApiKey(
                id = uuidProvider.getUuid(),
                projectId = project.id,
                apiKey = apiKey,
                createdAt = utcDateTimeProvider.getUtcDateTime()
            )
        )
    }
}
