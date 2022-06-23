package com.ampnet.blockchainapiservice.config

import com.ampnet.blockchainapiservice.config.binding.ProjectApiKeyResolver
import com.ampnet.blockchainapiservice.config.binding.RpcUrlSpecResolver
import com.ampnet.blockchainapiservice.config.binding.UserIdentifierResolver
import com.ampnet.blockchainapiservice.repository.ApiKeyRepository
import com.ampnet.blockchainapiservice.repository.ProjectRepository
import com.ampnet.blockchainapiservice.repository.UserIdentifierRepository
import com.ampnet.blockchainapiservice.service.UuidProvider
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val uuidProvider: UuidProvider,
    private val userIdentifierRepository: UserIdentifierRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val projectRepository: ProjectRepository
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(RpcUrlSpecResolver())
        resolvers.add(UserIdentifierResolver(uuidProvider, userIdentifierRepository))
        resolvers.add(ProjectApiKeyResolver(apiKeyRepository, projectRepository))
    }
}
