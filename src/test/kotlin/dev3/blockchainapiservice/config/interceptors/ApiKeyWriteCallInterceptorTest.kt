package dev3.blockchainapiservice.config.interceptors

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.config.CustomHeaders
import dev3.blockchainapiservice.config.JsonConfig
import dev3.blockchainapiservice.config.interceptors.annotation.ApiWriteLimitedMapping
import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import dev3.blockchainapiservice.exception.ErrorCode
import dev3.blockchainapiservice.exception.ErrorResponse
import dev3.blockchainapiservice.model.result.ApiKey
import dev3.blockchainapiservice.repository.ApiKeyRepository
import dev3.blockchainapiservice.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.repository.UserIdResolverRepository
import dev3.blockchainapiservice.service.UtcDateTimeProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerMapping
import java.io.PrintWriter
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.mockito.kotlin.verify as verifyMock

class ApiKeyWriteCallInterceptorTest : TestBase() {

    companion object {
        // @formatter:off
        @Suppress("unused")
        fun nonAnnotated() {}
        @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/test-path")
        @Suppress("unused")
        fun projectIdAnnotated() {}
        @ApiWriteLimitedMapping(IdType.ASSET_SEND_REQUEST_ID, RequestMethod.POST, "/test-path/{id}")
        @Suppress("unused", "UNUSED_PARAMETER")
        fun otherIdAnnotated(@PathVariable id: String) {}
        // @formatter:on

        private val OBJECT_MAPPER = JsonConfig().objectMapper()
        private val PATH_VARIABLES = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
    }

    @Test
    fun mustNotHandleNonAnnotatedMethod() {
        val apiKeyRepository = mock<ApiKeyRepository>()
        val apiRateLimitRepository = mock<ApiRateLimitRepository>()
        val userIdResolverRepository = mock<UserIdResolverRepository>()
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        val handler = HandlerMethod(Companion, Companion::class.java.methods.find { it.name == "nonAnnotated" }!!)
        val interceptor = ApiKeyWriteCallInterceptor(
            apiKeyRepository = apiKeyRepository,
            apiRateLimitRepository = apiRateLimitRepository,
            userIdResolverRepository = userIdResolverRepository,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = OBJECT_MAPPER
        )

        val request = mock<HttpServletRequest>()
        val response = mock<HttpServletResponse>()

        verify("unannotated method is not handled") {
            interceptor.preHandle(request, response, handler)
            interceptor.afterCompletion(request, response, handler, null)

            verifyNoInteractions(apiKeyRepository)
            verifyNoInteractions(apiRateLimitRepository)
            verifyNoInteractions(userIdResolverRepository)
            verifyNoInteractions(utcDateTimeProvider)
            verifyNoInteractions(request)
            verifyNoInteractions(response)
        }
    }

    @Test
    fun mustCorrectlyHandleAnnotatedMethodWhenThereIsSomeRemainingWriteLimitAndReturnStatusIsSuccessForApiKey() {
        val apiKey = "api-key"
        val request = mock<HttpServletRequest>()

        suppose("request will return API key header") {
            given(request.getHeader(CustomHeaders.API_KEY_HEADER))
                .willReturn(apiKey)
        }

        val projectId = UUID.randomUUID()
        val apiKeyRepository = mock<ApiKeyRepository>()

        suppose("API key repository will return some API key") {
            given(apiKeyRepository.getByValue(apiKey))
                .willReturn(
                    ApiKey(
                        id = UUID.randomUUID(),
                        projectId = projectId,
                        apiKey = apiKey,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }

        val userIdResolverRepository = mock<UserIdResolverRepository>()
        val userId = UUID.randomUUID()

        suppose("userId will be resolved in the repository") {
            given(userIdResolverRepository.getUserId(IdType.PROJECT_ID, projectId))
                .willReturn(userId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val apiRateLimitRepository = mock<ApiRateLimitRepository>()

        suppose("remaining read API rate limit will not be zero") {
            given(apiRateLimitRepository.remainingWriteLimit(userId, TestData.TIMESTAMP))
                .willReturn(1)
        }

        val response = mock<HttpServletResponse>()

        suppose("response will have successful status") {
            given(response.status)
                .willReturn(HttpStatus.OK.value())
        }

        val handler = HandlerMethod(Companion, Companion::class.java.methods.find { it.name == "projectIdAnnotated" }!!)
        val interceptor = ApiKeyWriteCallInterceptor(
            apiKeyRepository = apiKeyRepository,
            apiRateLimitRepository = apiRateLimitRepository,
            userIdResolverRepository = userIdResolverRepository,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = OBJECT_MAPPER
        )

        verify("annotated method is correctly handled") {
            val handleResult = interceptor.preHandle(request, response, handler)

            assertThat(handleResult).withMessage()
                .isTrue()

            interceptor.afterCompletion(request, response, handler, null)

            verifyMock(apiKeyRepository, times(2))
                .getByValue(apiKey)
            verifyNoMoreInteractions(apiKeyRepository)

            verifyMock(apiRateLimitRepository)
                .remainingWriteLimit(userId, TestData.TIMESTAMP)
            verifyMock(apiRateLimitRepository)
                .addWriteCall(userId, TestData.TIMESTAMP, RequestMethod.POST, "/test-path")
            verifyNoMoreInteractions(apiRateLimitRepository)

            verifyMock(userIdResolverRepository, times(2))
                .getUserId(IdType.PROJECT_ID, projectId)
            verifyNoMoreInteractions(userIdResolverRepository)
        }
    }

    @Test
    fun mustCorrectlyHandleAnnotatedMethodWhenThereIsSomeRemainingWriteLimitAndReturnStatusIsSuccessForPathId() {
        val projectId = UUID.randomUUID()
        val idType = IdType.ASSET_SEND_REQUEST_ID
        val request = mock<HttpServletRequest>()

        suppose("request will contain id") {
            given(request.getAttribute(PATH_VARIABLES))
                .willReturn(mapOf(idType.idVariableName to projectId.toString()))
        }

        val userIdResolverRepository = mock<UserIdResolverRepository>()
        val userId = UUID.randomUUID()

        suppose("userId will be resolved in the repository") {
            given(userIdResolverRepository.getUserId(idType, projectId))
                .willReturn(userId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val apiRateLimitRepository = mock<ApiRateLimitRepository>()

        suppose("remaining read API rate limit will not be zero") {
            given(apiRateLimitRepository.remainingWriteLimit(userId, TestData.TIMESTAMP))
                .willReturn(1)
        }

        val response = mock<HttpServletResponse>()

        suppose("response will have successful status") {
            given(response.status)
                .willReturn(HttpStatus.OK.value())
        }

        val apiKeyRepository = mock<ApiKeyRepository>()

        val handler = HandlerMethod(Companion, Companion::class.java.methods.find { it.name == "otherIdAnnotated" }!!)
        val interceptor = ApiKeyWriteCallInterceptor(
            apiKeyRepository = apiKeyRepository,
            apiRateLimitRepository = apiRateLimitRepository,
            userIdResolverRepository = userIdResolverRepository,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = OBJECT_MAPPER
        )

        verify("annotated method is correctly handled") {
            val handleResult = interceptor.preHandle(request, response, handler)

            assertThat(handleResult).withMessage()
                .isTrue()

            interceptor.afterCompletion(request, response, handler, null)

            verifyNoInteractions(apiKeyRepository)

            verifyMock(apiRateLimitRepository)
                .remainingWriteLimit(userId, TestData.TIMESTAMP)
            verifyMock(apiRateLimitRepository)
                .addWriteCall(userId, TestData.TIMESTAMP, RequestMethod.POST, "/test-path/{id}")
            verifyNoMoreInteractions(apiRateLimitRepository)

            verifyMock(userIdResolverRepository, times(2))
                .getUserId(idType, projectId)
            verifyNoMoreInteractions(userIdResolverRepository)
        }
    }

    @Test
    fun mustCorrectlyHandleAnnotatedMethodWhenThereIsNoRemainingWriteLimitAndReturnStatusIsNonSuccess() {
        val projectId = UUID.randomUUID()
        val idType = IdType.ASSET_SEND_REQUEST_ID
        val request = mock<HttpServletRequest>()

        suppose("request will contain id") {
            given(request.getAttribute(PATH_VARIABLES))
                .willReturn(mapOf(idType.idVariableName to projectId.toString()))
        }

        val userIdResolverRepository = mock<UserIdResolverRepository>()
        val userId = UUID.randomUUID()

        suppose("userId will be resolved in the repository") {
            given(userIdResolverRepository.getUserId(idType, projectId))
                .willReturn(userId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val apiRateLimitRepository = mock<ApiRateLimitRepository>()

        suppose("remaining read API rate limit will be zero") {
            given(apiRateLimitRepository.remainingWriteLimit(userId, TestData.TIMESTAMP))
                .willReturn(0)
        }

        val response = mock<HttpServletResponse>()

        suppose("response will have bad request status") {
            given(response.status)
                .willReturn(HttpStatus.BAD_REQUEST.value())
        }

        val writer = mock<PrintWriter>()

        suppose("response will return a writer") {
            given(response.writer)
                .willReturn(writer)
        }

        val apiKeyRepository = mock<ApiKeyRepository>()

        val handler = HandlerMethod(Companion, Companion::class.java.methods.find { it.name == "otherIdAnnotated" }!!)
        val interceptor = ApiKeyWriteCallInterceptor(
            apiKeyRepository = apiKeyRepository,
            apiRateLimitRepository = apiRateLimitRepository,
            userIdResolverRepository = userIdResolverRepository,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = OBJECT_MAPPER
        )

        verify("annotated method is correctly handled") {
            val handleResult = interceptor.preHandle(request, response, handler)

            assertThat(handleResult).withMessage()
                .isFalse()

            interceptor.afterCompletion(request, response, handler, null)

            verifyNoInteractions(apiKeyRepository)

            verifyMock(apiRateLimitRepository)
                .remainingWriteLimit(userId, TestData.TIMESTAMP)
            verifyNoMoreInteractions(apiRateLimitRepository)

            verifyMock(userIdResolverRepository, times(2))
                .getUserId(idType, projectId)
            verifyNoMoreInteractions(userIdResolverRepository)

            verifyMock(response)
                .status = HttpStatus.PAYMENT_REQUIRED.value()
            verifyMock(response)
                .writer
            verifyMock(response)
                .status
            verifyNoMoreInteractions(response)

            verifyMock(writer)
                .println(
                    OBJECT_MAPPER.writeValueAsString(
                        ErrorResponse(
                            errorCode = ErrorCode.API_RATE_LIMIT_EXCEEDED,
                            message = "API rate limit exceeded for write requests"
                        )
                    )
                )
            verifyNoMoreInteractions(writer)
        }
    }
}
