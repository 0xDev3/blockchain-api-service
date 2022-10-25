package dev3.blockchainapiservice.model.request

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.config.validation.MaxJsonNodeChars
import dev3.blockchainapiservice.config.validation.MaxStringSize
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import dev3.blockchainapiservice.exception.MissingTokenAddressException
import dev3.blockchainapiservice.exception.TokenAddressNotAllowedException
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.util.AssetType
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
    val disperseScreenConfig: ScreenConfig?
) {
    init {
        when (assetType) {
            AssetType.NATIVE -> if (tokenAddress != null) throw TokenAddressNotAllowedException()
            AssetType.TOKEN -> if (tokenAddress == null) throw MissingTokenAddressException()
        }
    }
}
