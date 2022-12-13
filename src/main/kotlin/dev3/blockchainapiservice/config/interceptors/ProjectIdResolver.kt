package dev3.blockchainapiservice.config.interceptors

import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import dev3.blockchainapiservice.repository.ProjectIdResolverRepository
import mu.KLogging
import org.springframework.web.servlet.HandlerMapping
import java.util.UUID
import javax.servlet.http.HttpServletRequest

object ProjectIdResolver : KLogging() {

    private val PATH_VARIABLES = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE

    @Suppress("UNCHECKED_CAST")
    fun resolve(
        projectIdResolverRepository: ProjectIdResolverRepository,
        interceptorName: String,
        request: HttpServletRequest,
        idType: IdType,
        path: String
    ): UUID? {
        val idVariable = (request.getAttribute(PATH_VARIABLES) as Map<String, String>)[idType.idVariableName]
            ?: throw IllegalStateException("$interceptorName is improperly configured for endpoint: $path")

        return idVariable.parseId()
            ?.let { projectIdResolverRepository.getProjectId(idType, it) }
    }

    fun parseUuid(uuid: String?): UUID? = uuid?.parseId()

    private fun String.parseId(): UUID? =
        try {
            UUID.fromString(this)
        } catch (_: IllegalArgumentException) {
            null
        }
}
