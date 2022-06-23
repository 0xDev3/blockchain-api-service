package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.UtcDateTime
import java.util.UUID

data class Project(
    val id: UUID,
    val ownerId: UUID,
    val issuerContractAddress: ContractAddress,
    val redirectUrl: String,
    val chainId: ChainId,
    val customRpcUrl: String?,
    val createdAt: UtcDateTime
)
