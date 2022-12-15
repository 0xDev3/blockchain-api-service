package dev3.blockchainapiservice.config.interceptors

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import dev3.blockchainapiservice.repository.UserIdResolverRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.web.servlet.HandlerMapping
import java.util.UUID
import javax.servlet.http.HttpServletRequest

class UserIdResolverTest : TestBase() {

    companion object {
        private val PATH_VARIABLES = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
    }

    @Test
    fun mustCorrectlyResolveSomeIdToUserId() {
        val id = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val idType = IdType.ASSET_SEND_REQUEST_ID
        val userIdResolverRepository = mock<UserIdResolverRepository>()

        suppose("userId will be resolved in the repository") {
            given(userIdResolverRepository.getUserId(idType, id))
                .willReturn(userId)
        }

        val request = mock<HttpServletRequest>()

        suppose("request will contain id") {
            given(request.getAttribute(PATH_VARIABLES))
                .willReturn(mapOf(idType.idVariableName to id.toString()))
        }

        verify("userId is correctly resolved") {
            val resolvedUserId = UserIdResolver.resolve(
                userIdResolverRepository = userIdResolverRepository,
                interceptorName = "test",
                request = request,
                idType = idType,
                path = "/test-path/{${idType.idVariableName}}/rest"
            )

            assertThat(resolvedUserId).withMessage()
                .isEqualTo(userId)
        }
    }

    @Test
    fun mustThrowIllegalStateExceptionWhenIdIsNotPresentInTheRequest() {
        val id = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val idType = IdType.ASSET_SEND_REQUEST_ID
        val userIdResolverRepository = mock<UserIdResolverRepository>()

        suppose("userId will be resolved in the repository") {
            given(userIdResolverRepository.getUserId(idType, id))
                .willReturn(userId)
        }

        val request = mock<HttpServletRequest>()

        suppose("request will not contain id") {
            given(request.getAttribute(PATH_VARIABLES))
                .willReturn(emptyMap<String, String>())
        }

        verify("IllegalStateException is thrown") {
            assertThrows<IllegalStateException>(message) {
                UserIdResolver.resolve(
                    userIdResolverRepository = userIdResolverRepository,
                    interceptorName = "test",
                    request = request,
                    idType = idType,
                    path = "/test-path/{${idType.idVariableName}}/rest"
                )
            }
        }
    }

    @Test
    fun mustReturnNullWhenRequestIdIsNotParsable() {
        val idType = IdType.ASSET_SEND_REQUEST_ID
        val request = mock<HttpServletRequest>()

        suppose("request will contain id") {
            given(request.getAttribute(PATH_VARIABLES))
                .willReturn(mapOf(idType.idVariableName to "invalid-id"))
        }

        verify("userId is correctly resolved") {
            val resolvedUserId = UserIdResolver.resolve(
                userIdResolverRepository = mock(),
                interceptorName = "test",
                request = request,
                idType = idType,
                path = "/test-path/{${idType.idVariableName}}/rest"
            )

            assertThat(resolvedUserId).withMessage()
                .isNull()
        }
    }
}