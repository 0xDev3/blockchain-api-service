package dev3.blockchainapiservice.config

import com.fasterxml.jackson.databind.ObjectMapper
import dev3.blockchainapiservice.config.binding.ProjectApiKeyResolver
import dev3.blockchainapiservice.config.binding.UserIdentifierResolver
import dev3.blockchainapiservice.config.interceptors.ApiKeyWriteCallInterceptor
import dev3.blockchainapiservice.config.interceptors.ProjectReadCallInterceptor
import dev3.blockchainapiservice.repository.ApiKeyRepository
import dev3.blockchainapiservice.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.repository.ProjectIdResolverRepository
import dev3.blockchainapiservice.repository.ProjectRepository
import dev3.blockchainapiservice.repository.UserIdentifierRepository
import dev3.blockchainapiservice.service.UtcDateTimeProvider
import dev3.blockchainapiservice.service.UuidProvider
import org.springframework.beans.factory.BeanCreationException
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val userIdentifierRepository: UserIdentifierRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val apiRateLimitRepository: ApiRateLimitRepository,
    private val projectIdResolverRepository: ProjectIdResolverRepository,
    private val projectRepository: ProjectRepository,
    private val objectMapper: ObjectMapper
) : WebMvcConfigurer {

    companion object {
        private const val MISSING_PROPERTY_MESSAGE =
            "application property blockchain-api-service.contract-manifest-service.base-url is not set"
    }

    @Bean("externalContractDecompilerServiceRestTemplate")
    fun externalContractDecompilerServiceRestTemplate(applicationProperties: ApplicationProperties): RestTemplate =
        RestTemplateBuilder()
            .rootUri(
                applicationProperties.contractManifestService.baseUrl
                    ?: throw BeanCreationException(MISSING_PROPERTY_MESSAGE)
            )
            .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
            .build()

    @Bean("pinataRestTemplate")
    fun pinataRestTemplate(applicationProperties: ApplicationProperties): RestTemplate =
        RestTemplateBuilder()
            .rootUri(applicationProperties.ipfs.url)
            .defaultHeader("pinata_api_key", applicationProperties.ipfs.apiKey)
            .defaultHeader("pinata_secret_api_key", applicationProperties.ipfs.secretApiKey)
            .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
            .build()

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(UserIdentifierResolver(uuidProvider, userIdentifierRepository))
        resolvers.add(ProjectApiKeyResolver(apiKeyRepository, projectRepository))
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(
            ApiKeyWriteCallInterceptor(
                apiKeyRepository = apiKeyRepository,
                apiRateLimitRepository = apiRateLimitRepository,
                projectIdResolverRepository = projectIdResolverRepository,
                utcDateTimeProvider = utcDateTimeProvider,
                objectMapper = objectMapper
            )
        )
        registry.addInterceptor(
            ProjectReadCallInterceptor(
                apiRateLimitRepository = apiRateLimitRepository,
                projectIdResolverRepository = projectIdResolverRepository,
                utcDateTimeProvider = utcDateTimeProvider,
                objectMapper = objectMapper
            )
        )
    }
}
