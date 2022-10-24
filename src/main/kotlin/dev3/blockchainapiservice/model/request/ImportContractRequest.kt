package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.config.validation.MaxJsonNodeChars
import com.ampnet.blockchainapiservice.config.validation.MaxStringSize
import com.ampnet.blockchainapiservice.config.validation.ValidAlias
import com.ampnet.blockchainapiservice.config.validation.ValidEthAddress
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.fasterxml.jackson.databind.JsonNode
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class ImportContractRequest(
    @field:NotNull
    @field:ValidAlias
    val alias: String,
    @field:MaxStringSize
    val contractId: String?,
    @field:NotNull
    @field:ValidEthAddress
    val contractAddress: String,
    @field:MaxStringSize
    val redirectUrl: String?,
    @field:MaxJsonNodeChars
    val arbitraryData: JsonNode?,
    @field:Valid
    val screenConfig: ScreenConfig?
)
