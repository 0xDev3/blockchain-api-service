package dev3.blockchainapiservice.config.binding

import dev3.blockchainapiservice.config.CustomHeaders
import dev3.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import dev3.blockchainapiservice.exception.NonExistentApiKeyException
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.repository.ApiKeyRepository
import dev3.blockchainapiservice.repository.ProjectRepository
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import javax.servlet.http.HttpServletRequest

class ProjectApiKeyResolver(
    private val apiKeyRepository: ApiKeyRepository,
    private val projectRepository: ProjectRepository
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.parameterType == Project::class.java &&
            parameter.hasParameterAnnotation(ApiKeyBinding::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        nativeWebRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Project {
        val httpServletRequest = nativeWebRequest.getNativeRequest(HttpServletRequest::class.java)
        // TODO check if API has expired/used up/etc. - out of scope for MVP
        val apiKey = httpServletRequest?.getHeader(CustomHeaders.API_KEY_HEADER)
            ?.let { apiKeyRepository.getByValue(it) }
            ?: throw NonExistentApiKeyException()
        return projectRepository.getById(apiKey.projectId)!! // non-null enforced by foreign key constraint in DB
    }
}
