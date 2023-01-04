package dev3.blockchainapiservice.features.contract.arbitrarycall.model.params

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.util.ContractAddress

data class PreStoreContractArbitraryCallRequestParams(
    val createParams: CreateContractArbitraryCallRequestParams,
    val deployedContractId: ContractDeploymentRequestId?,
    val functionName: String?,
    val functionParams: JsonNode?,
    val contractAddress: ContractAddress
)
