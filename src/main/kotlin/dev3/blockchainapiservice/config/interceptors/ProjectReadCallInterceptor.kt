package dev3.blockchainapiservice.config.interceptors

import dev3.blockchainapiservice.config.interceptors.annotation.ApiReadLimitedMapping
import dev3.blockchainapiservice.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.repository.UserIdResolverRepository
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
    private val userIdResolverRepository: UserIdResolverRepository,
    private val utcDateTimeProvider: UtcDateTimeProvider
) : HandlerInterceptor {

    companion object : KLogging()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean =
        handleAnnotatedMethod(request, handler) { _, _ -> // TODO not limited for now
//            val usedReadRequests = apiRateLimitRepository.usedReadRequests(userId)
//            val remainingReadLimit = 1L

//            if (remainingReadLimit > 0) {
            true
//            } else {
//                logger.warn { "User API limit exceeded: $userId" }
//
//                response.status = HttpStatus.PAYMENT_REQUIRED.value()
//                response.writer.println(
//                    objectMapper.writeValueAsString(
//                        ErrorResponse(
//                            errorCode = ErrorCode.API_RATE_LIMIT_EXCEEDED,
//                            message = "API rate limit exceeded for read requests"
//                        )
//                    )
//                )
//
//                false
//            }
        }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        handleAnnotatedMethod(request, handler) { userId, annotation ->
            if (HttpStatus.resolve(response.status)?.is2xxSuccessful == true) {
                apiRateLimitRepository.addReadCall(
                    userId = userId,
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
            UserIdResolver.resolve(
                userIdResolverRepository = userIdResolverRepository,
                interceptorName = "ProjectReadCallInterceptor",
                request = request,
                idType = annotation.idType,
                path = annotation.path
            )?.let { handle(it, annotation) } ?: true
        } else true
    }
}
