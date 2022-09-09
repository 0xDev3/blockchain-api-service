package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.config.validation.MaxStringSize
import com.ampnet.blockchainapiservice.config.validation.ValidEthAddress
import com.ampnet.blockchainapiservice.config.validation.ValidUint256
import com.ampnet.blockchainapiservice.exception.MissingTokenAddressException
import com.ampnet.blockchainapiservice.exception.TokenAddressNotAllowedException
import com.ampnet.blockchainapiservice.util.AssetType
import java.math.BigInteger
import javax.validation.constraints.NotNull

data class MultiPaymentTemplateItemRequest(
    @field:NotNull
    @field:ValidEthAddress
    val walletAddress: String,
    @field:MaxStringSize
    val itemName: String?,
    @field:NotNull
    val assetType: AssetType,
    @field:ValidEthAddress
    val tokenAddress: String?,
    @field:NotNull
    @field:ValidUint256
    val amount: BigInteger
) {
    init {
        when (assetType) {
            AssetType.NATIVE -> if (tokenAddress != null) throw TokenAddressNotAllowedException()
            AssetType.TOKEN -> if (tokenAddress == null) throw MissingTokenAddressException()
        }
    }
}
