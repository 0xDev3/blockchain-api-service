package dev3.blockchainapiservice.model.response

import dev3.blockchainapiservice.model.result.Project
import java.time.OffsetDateTime
import java.util.UUID

data class ProjectResponse(
    val id: UUID,
    val ownerId: UUID,
    val issuerContractAddress: String,
    val baseRedirectUrl: String,
    val chainId: Long,
    val customRpcUrl: String?,
    val createdAt: OffsetDateTime
) {
    constructor(project: Project) : this(
        id = project.id,
        ownerId = project.ownerId,
        issuerContractAddress = project.issuerContractAddress.rawValue,
        baseRedirectUrl = project.baseRedirectUrl.value,
        chainId = project.chainId.value,
        customRpcUrl = project.customRpcUrl,
        createdAt = project.createdAt.value
    )
}
