package dev3.blockchainapiservice.config

import com.fasterxml.jackson.databind.ObjectMapper
import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.config.binding.ProjectApiKeyResolver
import dev3.blockchainapiservice.config.binding.UserIdentifierResolver
import dev3.blockchainapiservice.config.interceptors.ApiKeyWriteCallInterceptor
import dev3.blockchainapiservice.config.interceptors.CorrelationIdInterceptor
import dev3.blockchainapiservice.config.interceptors.ProjectReadCallInterceptor
import dev3.blockchainapiservice.repository.ApiKeyRepository
import dev3.blockchainapiservice.repository.ApiRateLimitRepository
import dev3.blockchainapiservice.repository.ProjectRepository
import dev3.blockchainapiservice.repository.UserIdResolverRepository
import dev3.blockchainapiservice.repository.UserIdentifierRepository
import dev3.blockchainapiservice.service.FunctionEncoderService
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

@Suppress("LongParameterList")
@Configuration
class WebConfig(
    private val blockchainService: BlockchainService,
    private val functionEncoderService: FunctionEncoderService,
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val userIdentifierRepository: UserIdentifierRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val apiRateLimitRepository: ApiRateLimitRepository,
    private val userIdResolverRepository: UserIdResolverRepository,
    private val projectRepository: ProjectRepository,
    private val objectMapper: ObjectMapper,
    private val applicationProperties: ApplicationProperties
) : WebMvcConfigurer {

    companion object {
        private const val MISSING_PROPERTY_MESSAGE =
            "application property blockchain-api-service.contract-manifest-service.base-url is not set"
    }

    @Bean("externalContractDecompilerServiceRestTemplate")
    fun externalContractDecompilerServiceRestTemplate(
        objectMapper: ObjectMapper,
        applicationProperties: ApplicationProperties
    ): RestTemplate = RestTemplateBuilder()
        .rootUri(
            applicationProperties.contractManifestService.baseUrl
                ?: throw BeanCreationException(MISSING_PROPERTY_MESSAGE)
        )
        .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
        .build()

    @Bean("pinataRestTemplate")
    fun pinataRestTemplate(
        objectMapper: ObjectMapper,
        applicationProperties: ApplicationProperties
    ): RestTemplate = RestTemplateBuilder()
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
        registry.addInterceptor(CorrelationIdInterceptor(uuidProvider))
        registry.addInterceptor(
            ApiKeyWriteCallInterceptor(
                blockchainService = blockchainService,
                functionEncoderService = functionEncoderService,
                apiKeyRepository = apiKeyRepository,
                apiRateLimitRepository = apiRateLimitRepository,
                userIdResolverRepository = userIdResolverRepository,
                userIdentifierRepository = userIdentifierRepository,
                utcDateTimeProvider = utcDateTimeProvider,
                objectMapper = objectMapper,
                apiUsageProperties = applicationProperties.apiUsage
            )
        )
        registry.addInterceptor(
            ProjectReadCallInterceptor(
                apiRateLimitRepository = apiRateLimitRepository,
                userIdResolverRepository = userIdResolverRepository,
                utcDateTimeProvider = utcDateTimeProvider
            )
        )
    }
}
