package dev3.blockchainapiservice.config.binding

import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.config.CustomHeaders
import dev3.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import dev3.blockchainapiservice.exception.NonExistentApiKeyException
import dev3.blockchainapiservice.features.api.access.model.result.ApiKey
import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.api.access.repository.ApiKeyRepository
import dev3.blockchainapiservice.features.api.access.repository.ProjectRepository
import dev3.blockchainapiservice.generated.jooq.id.ApiKeyId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import org.junit.jupiter.api.Test
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

            expectThat(resolver.supportsParameter(parameter))
                .isTrue()
        }
    }

    @Test
    fun mustNotSupportUnannotatedProjectParameter() {
        val resolver = ProjectApiKeyResolver(mock(), mock())

        verify("annotated Project parameter is supported") {
            val method = Companion::class.java.methods.find { it.name == "unsupportedMethod1" }!!
            val parameter = MethodParameter(method, 0)

            expectThat(resolver.supportsParameter(parameter))
                .isFalse()
        }
    }

    @Test
    fun mustNotSupportAnnotatedNonProjectParameter() {
        val resolver = ProjectApiKeyResolver(mock(), mock())

        verify("annotated Project parameter is supported") {
            val method = Companion::class.java.methods.find { it.name == "unsupportedMethod2" }!!
            val parameter = MethodParameter(method, 0)

            expectThat(resolver.supportsParameter(parameter))
                .isFalse()
        }
    }

    @Test
    fun mustCorrectlyFetchExistingProjectByApiKey() {
        val apiKeyValue = "api-key"
        val httpServletRequest = mock<HttpServletRequest>()

        suppose("API key will be returned from header") {
            call(httpServletRequest.getHeader(CustomHeaders.API_KEY_HEADER))
                .willReturn(apiKeyValue)
        }

        val nativeWebRequest = mock<NativeWebRequest>()

        suppose("HttpServletRequest will be returned") {
            call(nativeWebRequest.getNativeRequest(HttpServletRequest::class.java))
                .willReturn(httpServletRequest)
        }

        val apiKeyRepository = mock<ApiKeyRepository>()
        val apiKey = ApiKey(
            id = ApiKeyId(UUID.randomUUID()),
            projectId = ProjectId(UUID.randomUUID()),
            apiKey = apiKeyValue,
            createdAt = TestData.TIMESTAMP
        )

        suppose("API key is fetched from database") {
            call(apiKeyRepository.getByValue(apiKeyValue))
                .willReturn(apiKey)
        }

        val projectRepository = mock<ProjectRepository>()
        val project = Project(
            id = apiKey.projectId,
            ownerId = UserId(UUID.randomUUID()),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )

        suppose("project is fetched from database") {
            call(projectRepository.getById(apiKey.projectId))
                .willReturn(project)
        }

        val resolver = ProjectApiKeyResolver(apiKeyRepository, projectRepository)

        verify("API key is correctly returned") {
            expectThat(resolver.resolveArgument(mock(), mock(), nativeWebRequest, mock()))
                .isEqualTo(project)
        }
    }

    @Test
    fun mustThrowNonExistentApiKeyExceptionForNonExistentApiKey() {
        val apiKeyValue = "api-key"
        val httpServletRequest = mock<HttpServletRequest>()

        suppose("API key will be returned from header") {
            call(httpServletRequest.getHeader(CustomHeaders.API_KEY_HEADER))
                .willReturn(apiKeyValue)
        }

        val nativeWebRequest = mock<NativeWebRequest>()

        suppose("HttpServletRequest will be returned") {
            call(nativeWebRequest.getNativeRequest(HttpServletRequest::class.java))
                .willReturn(httpServletRequest)
        }

        val repository = mock<ApiKeyRepository>()

        suppose("API key is null") {
            call(repository.getByValue(apiKeyValue))
                .willReturn(null)
        }

        val resolver = ProjectApiKeyResolver(repository, mock())

        verify("NonExistentApiKeyException is thrown") {
            expectThrows<NonExistentApiKeyException> {
                resolver.resolveArgument(mock(), mock(), nativeWebRequest, mock())
            }
        }
    }
}
