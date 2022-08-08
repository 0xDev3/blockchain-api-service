package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.config.validation.MaxJsonNodeChars
import com.ampnet.blockchainapiservice.config.validation.MaxStringSize
import com.ampnet.blockchainapiservice.config.validation.ValidEthAddress
import com.ampnet.blockchainapiservice.config.validation.ValidUint256
import com.ampnet.blockchainapiservice.exception.MissingTokenAddressException
import com.ampnet.blockchainapiservice.exception.TokenAddressNotAllowedException
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.AssetType
import com.fasterxml.jackson.databind.JsonNode
import java.math.BigInteger
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class CreateAssetSendRequest(
    @field:MaxStringSize
    val redirectUrl: String?,
    @field:ValidEthAddress
    val tokenAddress: String?,
    @field:NotNull
    val assetType: AssetType,
    @field:NotNull
    @field:ValidUint256
    val amount: BigInteger,
    @field:ValidEthAddress
    val senderAddress: String?,
    @field:NotNull
    @field:ValidEthAddress
    val recipientAddress: String,
    @field:MaxJsonNodeChars
    val arbitraryData: JsonNode?,
    @field:Valid
    val screenConfig: ScreenConfig?
) {
    init {
        when (assetType) {
            AssetType.NATIVE -> if (tokenAddress != null) throw TokenAddressNotAllowedException()
            AssetType.TOKEN -> if (tokenAddress == null) throw MissingTokenAddressException()
        }
    }
}
