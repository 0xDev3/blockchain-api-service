package dev3.blockchainapiservice.config.interceptors

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import dev3.blockchainapiservice.repository.ProjectIdResolverRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.web.servlet.HandlerMapping
import java.util.UUID
import javax.servlet.http.HttpServletRequest

class ProjectIdResolverTest : TestBase() {

    companion object {
        private val PATH_VARIABLES = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
    }

    @Test
    fun mustCorrectlyResolveSomeIdToProjectId() {
        val id = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val idType = IdType.ASSET_SEND_REQUEST_ID
        val projectIdResolverRepository = mock<ProjectIdResolverRepository>()

        suppose("projectId will be resolved in the repository") {
            given(projectIdResolverRepository.getProjectId(idType, id))
                .willReturn(projectId)
        }

        val request = mock<HttpServletRequest>()

        suppose("request will contain id") {
            given(request.getAttribute(PATH_VARIABLES))
                .willReturn(mapOf(idType.idVariableName to id.toString()))
        }

        verify("projectId is correctly resolved") {
            val resolvedProjectId = ProjectIdResolver.resolve(
                projectIdResolverRepository = projectIdResolverRepository,
                interceptorName = "test",
                request = request,
                idType = idType,
                path = "/test-path/{${idType.idVariableName}}/rest"
            )

            assertThat(resolvedProjectId).withMessage()
                .isEqualTo(projectId)
        }
    }

    @Test
    fun mustThrowIllegalStateExceptionWhenIdIsNotPresentInTheRequest() {
        val id = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val idType = IdType.ASSET_SEND_REQUEST_ID
        val projectIdResolverRepository = mock<ProjectIdResolverRepository>()

        suppose("projectId will be resolved in the repository") {
            given(projectIdResolverRepository.getProjectId(idType, id))
                .willReturn(projectId)
        }

        val request = mock<HttpServletRequest>()

        suppose("request will not contain id") {
            given(request.getAttribute(PATH_VARIABLES))
                .willReturn(emptyMap<String, String>())
        }

        verify("IllegalStateException is thrown") {
            assertThrows<IllegalStateException>(message) {
                ProjectIdResolver.resolve(
                    projectIdResolverRepository = projectIdResolverRepository,
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

        verify("projectId is correctly resolved") {
            val resolvedProjectId = ProjectIdResolver.resolve(
                projectIdResolverRepository = mock(),
                interceptorName = "test",
                request = request,
                idType = idType,
                path = "/test-path/{${idType.idVariableName}}/rest"
            )

            assertThat(resolvedProjectId).withMessage()
                .isNull()
        }
    }
}
