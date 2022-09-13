package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.config.validation.MaxStringSize
import com.ampnet.blockchainapiservice.config.validation.ValidEthAddress
import com.ampnet.blockchainapiservice.exception.MissingTokenAddressException
import com.ampnet.blockchainapiservice.exception.TokenAddressNotAllowedException
import com.ampnet.blockchainapiservice.util.AssetType
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class CreateMultiPaymentTemplateRequest(
    @field:NotNull
    @field:MaxStringSize
    val templateName: String,
    @field:NotNull
    val assetType: AssetType,
    @field:ValidEthAddress
    val tokenAddress: String?,
    @field:NotNull
    val chainId: Long,
    @field:NotNull
    @field:Valid
    val items: List<MultiPaymentTemplateItemRequest>
) {
    init {
        when (assetType) {
            AssetType.NATIVE -> if (tokenAddress != null) throw TokenAddressNotAllowedException()
            AssetType.TOKEN -> if (tokenAddress == null) throw MissingTokenAddressException()
        }
    }
}
