package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.config.validation.MaxJsonNodeChars
import com.ampnet.blockchainapiservice.config.validation.MaxStringSize
import com.ampnet.blockchainapiservice.config.validation.ValidEthAddress
import com.ampnet.blockchainapiservice.config.validation.ValidUint256
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.fasterxml.jackson.databind.JsonNode
import java.math.BigInteger
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class CreateErc20LockRequest(
    @field:MaxStringSize
    val redirectUrl: String?,
    @field:NotNull
    @field:ValidEthAddress
    val tokenAddress: String,
    @field:NotNull
    @field:ValidUint256
    val amount: BigInteger,
    @field:NotNull
    @field:ValidUint256
    val lockDurationInSeconds: BigInteger,
    @field:NotNull
    @field:ValidEthAddress
    val lockContractAddress: String,
    @field:ValidEthAddress
    val senderAddress: String?,
    @field:MaxJsonNodeChars
    val arbitraryData: JsonNode?,
    @field:Valid
    val screenConfig: ScreenConfig?
)
