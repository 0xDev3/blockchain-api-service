package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.UtcDateTime
import java.util.UUID

interface ParamsFactory<P, R> {
    fun fromCreateParams(id: UUID, params: P, project: Project, createdAt: UtcDateTime): R
    fun Project.createRedirectUrl(redirectUrl: String?, id: UUID, path: String) =
        (redirectUrl ?: (baseRedirectUrl.value + path)).replace("\${id}", id.toString())
}
