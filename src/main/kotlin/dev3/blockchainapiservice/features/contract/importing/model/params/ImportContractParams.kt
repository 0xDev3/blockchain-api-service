package dev3.blockchainapiservice.features.contract.importing.model.params

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.features.contract.importing.model.request.ImportContractRequest
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractId

data class ImportContractParams(
    val alias: String,
    val contractId: ContractId?,
    val contractAddress: ContractAddress,
    val redirectUrl: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
) {
    constructor(requestBody: ImportContractRequest) : this(
        alias = requestBody.alias,
        contractId = requestBody.contractId?.let { ContractId(it) },
        contractAddress = ContractAddress(requestBody.contractAddress),
        redirectUrl = requestBody.redirectUrl,
        arbitraryData = requestBody.arbitraryData,
        screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY
    )
}
