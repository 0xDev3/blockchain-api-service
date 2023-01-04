package dev3.blockchainapiservice.features.api.access.model.result

import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.UtcDateTime
import java.util.UUID

data class Project(
    val id: ProjectId,
    val ownerId: UserId,
    val issuerContractAddress: ContractAddress,
    val baseRedirectUrl: BaseUrl,
    val chainId: ChainId,
    val customRpcUrl: String?,
    val createdAt: UtcDateTime
) {
    fun createRedirectUrl(redirectUrl: String?, id: UUID, path: String) =
        (redirectUrl ?: (baseRedirectUrl.value + path)).replace("\${id}", id.toString())
}
