package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class StoreContractDeploymentRequestParams(
    val id: UUID,
    val contractId: ContractId,
    val contractData: ContractBinaryData,
    val contractTags: List<ContractTag>,
    val contractImplements: List<ContractTrait>,
    val chainId: ChainId,
    val redirectUrl: String,
    val projectId: UUID,
    val createdAt: UtcDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
)
