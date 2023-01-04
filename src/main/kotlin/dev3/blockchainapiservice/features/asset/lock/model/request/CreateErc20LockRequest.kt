package dev3.blockchainapiservice.features.asset.lock.model.request

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.config.validation.MaxJsonNodeChars
import dev3.blockchainapiservice.config.validation.MaxStringSize
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import dev3.blockchainapiservice.config.validation.ValidUint256
import dev3.blockchainapiservice.model.ScreenConfig
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
