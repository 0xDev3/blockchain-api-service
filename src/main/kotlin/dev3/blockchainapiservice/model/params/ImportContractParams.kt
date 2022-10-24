package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.request.ImportContractRequest
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.ContractId
import com.fasterxml.jackson.databind.JsonNode

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
