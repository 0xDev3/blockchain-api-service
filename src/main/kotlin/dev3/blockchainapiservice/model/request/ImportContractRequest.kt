package dev3.blockchainapiservice.model.request

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.config.validation.MaxJsonNodeChars
import dev3.blockchainapiservice.config.validation.MaxStringSize
import dev3.blockchainapiservice.config.validation.ValidAlias
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import dev3.blockchainapiservice.model.ScreenConfig
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
