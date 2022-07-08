package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class ContractDeploymentRequest(
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
    val screenBeforeActionMessage: String?,
    val screenAfterActionMessage: String?,
    val contractAddress: ContractAddress?,
    val deployerAddress: WalletAddress?,
    val txHash: TransactionHash?
)
