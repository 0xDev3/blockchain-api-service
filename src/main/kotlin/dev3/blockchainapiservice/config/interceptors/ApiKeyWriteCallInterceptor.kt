package dev3.blockchainapiservice.config.interceptors

import com.fasterxml.jackson.databind.ObjectMapper
import dev3.blockchainapiservice.config.CustomHeaders
import dev3.blockchainapiservice.config.interceptors.annotation.ApiWriteLimitedMapping
import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import dev3.blockchainapiservice.exception.ErrorCode
import dev3.blockchainapiservice.exception.ErrorResponse
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.repository.ApiKeyRepository
import dev3.blockchainapiservice.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.repository.UserIdResolverRepository
import dev3.blockchainapiservice.service.UtcDateTimeProvider
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ApiKeyWriteCallInterceptor(
    private val apiKeyRepository: ApiKeyRepository,
    private val apiRateLimitRepository: ApiRateLimitRepository,
    private val userIdResolverRepository: UserIdResolverRepository,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val objectMapper: ObjectMapper
) : HandlerInterceptor {

    companion object : KLogging()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean =
        handleAnnotatedMethod(request, handler) { userId, _ ->
            val remainingWriteLimit = apiRateLimitRepository.remainingWriteLimit(
                userId = userId,
                currentTime = utcDateTimeProvider.getUtcDateTime()
            )

            if (remainingWriteLimit > 0) {
                true
            } else {
                logger.warn { "API key rate limit exceeded for userId: $userId" }

                response.status = HttpStatus.PAYMENT_REQUIRED.value()
                response.writer.println(
                    objectMapper.writeValueAsString(
                        ErrorResponse(
                            errorCode = ErrorCode.API_RATE_LIMIT_EXCEEDED,
                            message = "API rate limit exceeded for write requests"
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
        handleAnnotatedMethod(request, handler) { userId, annotation ->
            if (HttpStatus.resolve(response.status)?.is2xxSuccessful == true) {
                apiRateLimitRepository.addWriteCall(
                    userId = userId,
                    currentTime = utcDateTimeProvider.getUtcDateTime(),
                    method = annotation.method,
                    endpoint = annotation.path
                )
            }

            true
        }
    }

    private fun handleAnnotatedMethod(
        request: HttpServletRequest,
        handler: Any,
        handle: (UserId, ApiWriteLimitedMapping) -> Boolean
    ): Boolean {
        val annotation = (handler as? HandlerMethod)?.method?.getAnnotation(ApiWriteLimitedMapping::class.java)

        return if (annotation != null) {
            annotation.resolveUserId(request)
                ?.let { handle(it, annotation) }
                ?: true
        } else true
    }

    private fun ApiWriteLimitedMapping.resolveUserId(request: HttpServletRequest): UserId? =
        if (idType == IdType.PROJECT_ID) {
            request.getHeader(CustomHeaders.API_KEY_HEADER)
                ?.let { apiKeyRepository.getByValue(it)?.projectId }
                ?.let { userIdResolverRepository.getByProjectId(it) }
        } else {
            UserIdResolver.resolve(
                userIdResolverRepository = userIdResolverRepository,
                interceptorName = "ApiKeyWriteCallInterceptor",
                request = request,
                idType = idType,
                path = path
            )
        }
}
