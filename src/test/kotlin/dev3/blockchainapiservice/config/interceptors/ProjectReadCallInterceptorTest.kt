package dev3.blockchainapiservice.config.interceptors

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.config.JsonConfig
import dev3.blockchainapiservice.config.interceptors.annotation.ApiReadLimitedMapping
import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import dev3.blockchainapiservice.exception.ErrorCode
import dev3.blockchainapiservice.exception.ErrorResponse
import dev3.blockchainapiservice.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.repository.ProjectIdResolverRepository
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
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerMapping
import java.io.PrintWriter
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.mockito.kotlin.verify as verifyMock

class ProjectReadCallInterceptorTest : TestBase() {

    companion object {
        // @formatter:off
        @Suppress("unused")
        fun nonAnnotated() {}
        @ApiReadLimitedMapping(IdType.PROJECT_ID, "/test-path/{projectId}")
        @Suppress("unused", "UNUSED_PARAMETER")
        fun annotated(@PathVariable projectId: String) {}
        // @formatter:on

        private val OBJECT_MAPPER = JsonConfig().objectMapper()
        private val PATH_VARIABLES = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
    }

    @Test
    fun mustNotHandleNonAnnotatedMethod() {
        val apiRateLimitRepository = mock<ApiRateLimitRepository>()
        val projectIdResolverRepository = mock<ProjectIdResolverRepository>()
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        val handler = HandlerMethod(Companion, Companion::class.java.methods.find { it.name == "nonAnnotated" }!!)
        val interceptor = ProjectReadCallInterceptor(
            apiRateLimitRepository = apiRateLimitRepository,
            projectIdResolverRepository = projectIdResolverRepository,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = OBJECT_MAPPER
        )

        val request = mock<HttpServletRequest>()
        val response = mock<HttpServletResponse>()

        verify("unannotated method is not handled") {
            interceptor.preHandle(request, response, handler)
            interceptor.afterCompletion(request, response, handler, null)

            verifyNoInteractions(apiRateLimitRepository)
            verifyNoInteractions(projectIdResolverRepository)
            verifyNoInteractions(utcDateTimeProvider)
            verifyNoInteractions(request)
            verifyNoInteractions(response)
        }
    }

    @Test
    fun mustCorrectlyHandleAnnotatedMethodWhenThereIsSomeRemainingReadLimitAndReturnStatusIsSuccess() {
        val projectId = UUID.randomUUID()
        val idType = IdType.PROJECT_ID
        val request = mock<HttpServletRequest>()

        suppose("request will contain id") {
            given(request.getAttribute(PATH_VARIABLES))
                .willReturn(mapOf(idType.idVariableName to projectId.toString()))
        }

        val projectIdResolverRepository = mock<ProjectIdResolverRepository>()

        suppose("projectId will be resolved in the repository") {
            given(projectIdResolverRepository.getProjectId(idType, projectId))
                .willReturn(projectId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val apiRateLimitRepository = mock<ApiRateLimitRepository>()

        suppose("remaining read API rate limit will not be zero") {
            given(apiRateLimitRepository.remainingReadLimit(projectId, TestData.TIMESTAMP))
                .willReturn(1)
        }

        val response = mock<HttpServletResponse>()

        suppose("response will have successful status") {
            given(response.status)
                .willReturn(HttpStatus.OK.value())
        }

        val handler = HandlerMethod(Companion, Companion::class.java.methods.find { it.name == "annotated" }!!)
        val interceptor = ProjectReadCallInterceptor(
            apiRateLimitRepository = apiRateLimitRepository,
            projectIdResolverRepository = projectIdResolverRepository,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = OBJECT_MAPPER
        )

        verify("annotated method is correctly handled") {
            val handleResult = interceptor.preHandle(request, response, handler)

            assertThat(handleResult).withMessage()
                .isTrue()

            interceptor.afterCompletion(request, response, handler, null)

            verifyMock(apiRateLimitRepository)
                .remainingReadLimit(projectId, TestData.TIMESTAMP)
            verifyMock(apiRateLimitRepository)
                .addReadCall(projectId, TestData.TIMESTAMP, "/test-path/{projectId}")
            verifyNoMoreInteractions(apiRateLimitRepository)

            verifyMock(projectIdResolverRepository, times(2))
                .getProjectId(idType, projectId)
            verifyNoMoreInteractions(projectIdResolverRepository)
        }
    }

    @Test
    fun mustCorrectlyHandleAnnotatedMethodWhenThereIsNoRemainingReadLimitAndReturnStatusIsNonSuccess() {
        val projectId = UUID.randomUUID()
        val idType = IdType.PROJECT_ID
        val request = mock<HttpServletRequest>()

        suppose("request will contain id") {
            given(request.getAttribute(PATH_VARIABLES))
                .willReturn(mapOf(idType.idVariableName to projectId.toString()))
        }

        val projectIdResolverRepository = mock<ProjectIdResolverRepository>()

        suppose("projectId will be resolved in the repository") {
            given(projectIdResolverRepository.getProjectId(idType, projectId))
                .willReturn(projectId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some date-time will be returned") {
            given(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val apiRateLimitRepository = mock<ApiRateLimitRepository>()

        suppose("remaining read API rate limit will be zero") {
            given(apiRateLimitRepository.remainingReadLimit(projectId, TestData.TIMESTAMP))
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

        val handler = HandlerMethod(Companion, Companion::class.java.methods.find { it.name == "annotated" }!!)
        val interceptor = ProjectReadCallInterceptor(
            apiRateLimitRepository = apiRateLimitRepository,
            projectIdResolverRepository = projectIdResolverRepository,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = OBJECT_MAPPER
        )

        verify("annotated method is correctly handled") {
            val handleResult = interceptor.preHandle(request, response, handler)

            assertThat(handleResult).withMessage()
                .isFalse()

            interceptor.afterCompletion(request, response, handler, null)

            verifyMock(apiRateLimitRepository)
                .remainingReadLimit(projectId, TestData.TIMESTAMP)
            verifyNoMoreInteractions(apiRateLimitRepository)

            verifyMock(projectIdResolverRepository, times(2))
                .getProjectId(idType, projectId)
            verifyNoMoreInteractions(projectIdResolverRepository)

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
                            message = "API rate limit exceeded for read requests"
                        )
                    )
                )
            verifyNoMoreInteractions(writer)
        }
    }
}
