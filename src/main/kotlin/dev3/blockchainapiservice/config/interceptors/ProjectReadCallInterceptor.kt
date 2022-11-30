package dev3.blockchainapiservice.config.interceptors

import com.fasterxml.jackson.databind.ObjectMapper
import dev3.blockchainapiservice.config.interceptors.annotation.ApiReadLimitedMapping
import dev3.blockchainapiservice.exception.ErrorCode
import dev3.blockchainapiservice.exception.ErrorResponse
import dev3.blockchainapiservice.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.repository.ProjectIdResolverRepository
import dev3.blockchainapiservice.service.UtcDateTimeProvider
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ProjectReadCallInterceptor(
    private val apiRateLimitRepository: ApiRateLimitRepository,
    private val projectIdResolverRepository: ProjectIdResolverRepository,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val objectMapper: ObjectMapper
) : HandlerInterceptor {

    companion object : KLogging()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean =
        handleAnnotatedMethod(request, handler) { projectId, _ ->
            val remainingReadLimit = apiRateLimitRepository.remainingReadLimit(
                projectId = projectId,
                currentTime = utcDateTimeProvider.getUtcDateTime()
            )

            if (remainingReadLimit > 0) {
                true
            } else {
                logger.warn { "Project limit exceeded: $projectId" }

                response.status = HttpStatus.PAYMENT_REQUIRED.value()
                response.writer.println(
                    objectMapper.writeValueAsString(
                        ErrorResponse(
                            errorCode = ErrorCode.API_RATE_LIMIT_EXCEEDED,
                            message = "API rate limit exceeded for read requests"
                        )
                    )
                )

                false
            }
        }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        handleAnnotatedMethod(request, handler) { projectId, annotation ->
            if (HttpStatus.resolve(response.status)?.is2xxSuccessful == true) {
                apiRateLimitRepository.addReadCall(
                    projectId = projectId,
                    currentTime = utcDateTimeProvider.getUtcDateTime(),
                    endpoint = annotation.path
                )
            }

            true
        }
    }

    private fun handleAnnotatedMethod(
        request: HttpServletRequest,
        handler: Any,
        handle: (UUID, ApiReadLimitedMapping) -> Boolean
    ): Boolean {
        val annotation = (handler as? HandlerMethod)?.method?.getAnnotation(ApiReadLimitedMapping::class.java)

        return if (annotation != null) {
            ProjectIdResolver.resolve(
                projectIdResolverRepository = projectIdResolverRepository,
                interceptorName = "ProjectReadCallInterceptor",
                request = request,
                idType = annotation.idType,
                path = annotation.path
            )?.let { handle(it, annotation) } ?: true
        } else true
    }
}
