package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.UtcDateTime
import java.util.UUID

data class Project(
    val id: UUID,
    val ownerId: UUID,
    val issuerContractAddress: ContractAddress,
    val baseRedirectUrl: BaseUrl,
    val chainId: ChainId,
    val customRpcUrl: String?,
    val createdAt: UtcDateTime
) {
    fun createRedirectUrl(redirectUrl: String?, id: UUID, path: String) =
        (redirectUrl ?: (baseRedirectUrl.value + path)).replace("\${id}", id.toString())
}
