package dev3.blockchainapiservice.features.contract.arbitrarycall.model.params

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.util.ContractAddress
import java.util.UUID

data class PreStoreContractArbitraryCallRequestParams(
    val createParams: CreateContractArbitraryCallRequestParams,
    val deployedContractId: UUID?,
    val functionName: String?,
    val functionParams: JsonNode?,
    val contractAddress: ContractAddress
)
