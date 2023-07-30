package dev3.blockchainapiservice.config.binding

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import dev3.blockchainapiservice.exception.NonExistentApiKeyException
import dev3.blockchainapiservice.model.result.ApiKey
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.ApiKeyRepository
import dev3.blockchainapiservice.repository.ProjectRepository
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest
import java.util.UUID
import javax.servlet.http.HttpServletRequest

class ProjectApiKeyResolverTest : TestBase() {

    companion object {
        // @formatter:off
        @Suppress("unused", "UNUSED_PARAMETER")
        fun supportedMethod(@ApiKeyBinding param: Project) {}
        @Suppress("unused", "UNUSED_PARAMETER")
        fun unsupportedMethod1(param: ApiKey) {}
        @Suppress("unused", "UNUSED_PARAMETER")
        fun unsupportedMethod2(@ApiKeyBinding param: String) {}
        // @formatter:on
    }

    @Test
    fun mustSupportAnnotatedProjectParameter() {
        val resolver = ProjectApiKeyResolver(mock(), mock())

        verify("annotated Project parameter is supported") {
            val method = Companion::class.java.methods.find { it.name == "supportedMethod" }!!
            val parameter = MethodParameter(method, 0)

            assertThat(resolver.supportsParameter(parameter)).withMessage()
                .isTrue()
        }
    }

    @Test
    fun mustNotSupportUnannotatedProjectParameter() {
        val resolver = ProjectApiKeyResolver(mock(), mock())

        verify("annotated Project parameter is supported") {
            val method = Companion::class.java.methods.find { it.name == "unsupportedMethod1" }!!
            val parameter = MethodParameter(method, 0)

            assertThat(resolver.supportsParameter(parameter)).withMessage()
                .isFalse()
        }
    }

    @Test
    fun mustNotSupportAnnotatedNonProjectParameter() {
        val resolver = ProjectApiKeyResolver(mock(), mock())

        verify("annotated Project parameter is supported") {
            val method = Companion::class.java.methods.find { it.name == "unsupportedMethod2" }!!
            val parameter = MethodParameter(method, 0)

            assertThat(resolver.supportsParameter(parameter)).withMessage()
                .isFalse()
        }
    }

    @Test
    fun mustCorrectlyFetchExistingProjectByApiKey() {
        val apiKeyValue = "api-key"
        val httpServletRequest = mock<HttpServletRequest>()

        suppose("API key will be returned from header") {
            given(httpServletRequest.getHeader(ProjectApiKeyResolver.API_KEY_HEADER))
                .willReturn(apiKeyValue)
        }

        val nativeWebRequest = mock<NativeWebRequest>()

        suppose("HttpServletRequest will be returned") {
            given(nativeWebRequest.getNativeRequest(HttpServletRequest::class.java))
                .willReturn(httpServletRequest)
        }

        val apiKeyRepository = mock<ApiKeyRepository>()
        val apiKey = ApiKey(
            id = UUID.randomUUID(),
            projectId = UUID.randomUUID(),
            apiKey = apiKeyValue,
            createdAt = TestData.TIMESTAMP
        )

        suppose("API key is fetched from database") {
            given(apiKeyRepository.getByValue(apiKeyValue))
                .willReturn(apiKey)
        }

        val projectRepository = mock<ProjectRepository>()
        val project = Project(
            id = apiKey.projectId,
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )

        suppose("project is fetched from database") {
            given(projectRepository.getById(apiKey.projectId))
                .willReturn(project)
        }

        val resolver = ProjectApiKeyResolver(apiKeyRepository, projectRepository)

        verify("API key is correctly returned") {
            assertThat(resolver.resolveArgument(mock(), mock(), nativeWebRequest, mock())).withMessage()
                .isEqualTo(project)
        }
    }

    @Test
    fun mustThrowNonExistentApiKeyExceptionForNonExistentApiKey() {
        val apiKeyValue = "api-key"
        val httpServletRequest = mock<HttpServletRequest>()

        suppose("API key will be returned from header") {
            given(httpServletRequest.getHeader(ProjectApiKeyResolver.API_KEY_HEADER))
                .willReturn(apiKeyValue)
        }

        val nativeWebRequest = mock<NativeWebRequest>()

        suppose("HttpServletRequest will be returned") {
            given(nativeWebRequest.getNativeRequest(HttpServletRequest::class.java))
                .willReturn(httpServletRequest)
        }

        val repository = mock<ApiKeyRepository>()

        suppose("API key is null") {
            given(repository.getByValue(apiKeyValue))
                .willReturn(null)
        }

        val resolver = ProjectApiKeyResolver(repository, mock())

        verify("NonExistentApiKeyException is thrown") {
            assertThrows<NonExistentApiKeyException>(message) {
                resolver.resolveArgument(mock(), mock(), nativeWebRequest, mock())
            }
        }
    }
}
