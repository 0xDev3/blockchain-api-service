package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import dev3.blockchainapiservice.config.binding.annotation.UserIdentifierBinding
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.response.ApiUsagePeriodResponse
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.model.result.UserIdentifier
import dev3.blockchainapiservice.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.repository.ProjectRepository
import dev3.blockchainapiservice.service.UtcDateTimeProvider
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Validated
@RestController
class ApiUsageController(
    private val projectRepository: ProjectRepository,
    private val apiRateLimitRepository: ApiRateLimitRepository,
    private val utcDateTimeProvider: UtcDateTimeProvider
) {

    @GetMapping("/v1/api-usage/by-project/{projectId}")
    fun getCurrentApiUsageInfoForProject(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @PathVariable projectId: UUID
    ): ResponseEntity<ApiUsagePeriodResponse> {
        val project = projectRepository.getById(projectId)
            ?.takeIf { it.ownerId == userIdentifier.id }
            ?: throw ResourceNotFoundException("Project does not exist for ID: $projectId")
        val usage = apiRateLimitRepository.getCurrentApiUsagePeriod(project.id, utcDateTimeProvider.getUtcDateTime())
        return ResponseEntity.ok(ApiUsagePeriodResponse(usage))
    }

    @GetMapping("/v1/api-usage")
    fun getCurrentApiUsageInfoForApiKey(
        @ApiKeyBinding project: Project
    ): ResponseEntity<ApiUsagePeriodResponse> {
        val usage = apiRateLimitRepository.getCurrentApiUsagePeriod(project.id, utcDateTimeProvider.getUtcDateTime())
        return ResponseEntity.ok(ApiUsagePeriodResponse(usage))
    }
}
