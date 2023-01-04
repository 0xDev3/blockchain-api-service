package dev3.blockchainapiservice.features.api.usage.controller

import dev3.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import dev3.blockchainapiservice.config.binding.annotation.UserIdentifierBinding
import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.api.access.model.result.UserIdentifier
import dev3.blockchainapiservice.features.api.usage.model.response.ApiUsagePeriodResponse
import dev3.blockchainapiservice.features.api.usage.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.service.UtcDateTimeProvider
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class ApiUsageController(
    private val apiRateLimitRepository: ApiRateLimitRepository,
    private val utcDateTimeProvider: UtcDateTimeProvider
) {

    @GetMapping("/v1/api-usage")
    fun getCurrentApiUsageInfoForUser(
        @UserIdentifierBinding userIdentifier: UserIdentifier
    ): ResponseEntity<ApiUsagePeriodResponse> {
        val usage = apiRateLimitRepository.getCurrentApiUsagePeriod(
            userId = userIdentifier.id,
            currentTime = utcDateTimeProvider.getUtcDateTime()
        )
        return ResponseEntity.ok(ApiUsagePeriodResponse(usage))
    }

    @GetMapping("/v1/api-usage/by-api-key")
    fun getCurrentApiUsageInfoForApiKey(
        @ApiKeyBinding project: Project
    ): ResponseEntity<ApiUsagePeriodResponse> {
        val usage = apiRateLimitRepository.getCurrentApiUsagePeriod(
            userId = project.ownerId,
            currentTime = utcDateTimeProvider.getUtcDateTime()
        )
        return ResponseEntity.ok(ApiUsagePeriodResponse(usage))
    }
}
