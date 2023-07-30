package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.config.binding.annotation.UserIdentifierBinding
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import dev3.blockchainapiservice.exception.ApiKeyAlreadyExistsException
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.params.CreateProjectParams
import dev3.blockchainapiservice.model.request.CreateProjectRequest
import dev3.blockchainapiservice.model.response.ApiKeyResponse
import dev3.blockchainapiservice.model.response.ProjectResponse
import dev3.blockchainapiservice.model.response.ProjectsResponse
import dev3.blockchainapiservice.model.result.UserIdentifier
import dev3.blockchainapiservice.service.AnalyticsService
import dev3.blockchainapiservice.service.ProjectService
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@Validated
@RestController
class ProjectController(
    private val projectService: ProjectService,
    private val analyticsService: AnalyticsService
) {

    @PostMapping("/v1/projects")
    fun createProject(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @Valid @RequestBody requestBody: CreateProjectRequest
    ): ResponseEntity<ProjectResponse> {
        val params = CreateProjectParams(
            issuerContractAddress = ContractAddress(requestBody.issuerContractAddress),
            baseRedirectUrl = BaseUrl(requestBody.baseRedirectUrl),
            chainId = ChainId(requestBody.chainId),
            customRpcUrl = requestBody.customRpcUrl
        )

        val project = projectService.createProject(userIdentifier, params)

        return ResponseEntity.ok(ProjectResponse(project))
    }

    @GetMapping("/v1/projects/{id}")
    fun getById(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @PathVariable id: UUID
    ): ResponseEntity<ProjectResponse> {
        val project = projectService.getProjectById(userIdentifier, id)
        return ResponseEntity.ok(ProjectResponse(project))
    }

    @GetMapping("/v1/projects/by-chain/{chainId}/by-issuer/{issuerAddress}")
    fun getByIssuerAddress(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @PathVariable chainId: Long,
        @ValidEthAddress @PathVariable issuerAddress: String
    ): ResponseEntity<ProjectResponse> {
        val project = projectService.getProjectByIssuer(
            userIdentifier = userIdentifier,
            issuerContactAddress = ContractAddress(issuerAddress),
            chainId = ChainId(chainId)
        )
        return ResponseEntity.ok(ProjectResponse(project))
    }

    @GetMapping("/v1/projects")
    fun getAll(@UserIdentifierBinding userIdentifier: UserIdentifier): ResponseEntity<ProjectsResponse> {
        val projects = projectService.getAllProjectsForUser(userIdentifier)
        return ResponseEntity.ok(ProjectsResponse(projects.map { ProjectResponse(it) }))
    }

    @GetMapping("/v1/projects/{id}/api-key")
    fun getApiKey(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @PathVariable id: UUID
    ): ResponseEntity<ApiKeyResponse> { // TODO return multiple API keys in the future
        val apiKey = projectService.getProjectApiKeys(userIdentifier, id).firstOrNull()
            ?: throw ResourceNotFoundException("API key not yet generated for provided project ID")
        return ResponseEntity.ok(ApiKeyResponse(apiKey))
    }

    @PostMapping("/v1/projects/{id}/api-key")
    fun createApiKey(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @PathVariable id: UUID,
        request: HttpServletRequest
    ): ResponseEntity<ApiKeyResponse> { // TODO allow multiple API key creation in the future
        if (projectService.getProjectApiKeys(userIdentifier, id).isNotEmpty()) {
            throw ApiKeyAlreadyExistsException(id)
        }

        val apiKey = projectService.createApiKey(userIdentifier, id)

        ignoreErrors {
            analyticsService.postApiKeyCreatedEvent(
                userIdentifier = userIdentifier,
                projectId = apiKey.projectId,
                origin = request.getHeader("Origin"),
                userAgent = request.getHeader("User-Agent"),
                remoteAddr = request.remoteAddr
            )
        }

        return ResponseEntity.ok(ApiKeyResponse(apiKey))
    }

    private fun ignoreErrors(call: () -> Unit) =
        try {
            call()
        } catch (_: Exception) {
        }
}
