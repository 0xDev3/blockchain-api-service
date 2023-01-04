package dev3.blockchainapiservice.features.asset.multisend.model.request

import dev3.blockchainapiservice.config.validation.MaxStringSize
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import dev3.blockchainapiservice.exception.MissingTokenAddressException
import dev3.blockchainapiservice.exception.TokenAddressNotAllowedException
import dev3.blockchainapiservice.util.AssetType
import javax.validation.constraints.NotNull

data class UpdateMultiPaymentTemplateRequest(
    @field:NotNull
    @field:MaxStringSize
    val templateName: String,
    @field:NotNull
    val assetType: AssetType,
    @field:ValidEthAddress
    val tokenAddress: String?,
    @field:NotNull
    val chainId: Long
) {
    init {
        when (assetType) {
            AssetType.NATIVE -> if (tokenAddress != null) throw TokenAddressNotAllowedException()
            AssetType.TOKEN -> if (tokenAddress == null) throw MissingTokenAddressException()
        }
    }
}
