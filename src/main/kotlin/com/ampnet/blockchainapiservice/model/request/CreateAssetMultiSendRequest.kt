package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.config.validation.MaxJsonNodeChars
import com.ampnet.blockchainapiservice.config.validation.MaxStringSize
import com.ampnet.blockchainapiservice.config.validation.ValidEthAddress
import com.ampnet.blockchainapiservice.exception.MissingTokenAddressException
import com.ampnet.blockchainapiservice.exception.TokenAddressNotAllowedException
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.AssetType
import com.fasterxml.jackson.databind.JsonNode
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class CreateAssetMultiSendRequest(
    @field:MaxStringSize
    val redirectUrl: String?,
    @field:ValidEthAddress
    val tokenAddress: String?,
    @field:NotNull
    @field:ValidEthAddress
    val disperseContractAddress: String,
    @field:NotNull
    val assetType: AssetType,
    @field:NotNull
    @field:Valid
    val items: List<MultiPaymentTemplateItemRequest>,
    @field:ValidEthAddress
    val senderAddress: String?,
    @field:MaxJsonNodeChars
    val arbitraryData: JsonNode?,
    @field:Valid
    val approveScreenConfig: ScreenConfig?,
    @field:Valid
    val sendScreenConfig: ScreenConfig?
) {
    init {
        when (assetType) {
            AssetType.NATIVE -> if (tokenAddress != null) throw TokenAddressNotAllowedException()
            AssetType.TOKEN -> if (tokenAddress == null) throw MissingTokenAddressException()
        }
    }
}
