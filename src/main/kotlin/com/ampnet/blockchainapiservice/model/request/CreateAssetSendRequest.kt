package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.exception.MissingTokenAddressException
import com.ampnet.blockchainapiservice.exception.TokenAddressNotAllowedException
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.AssetType
import com.fasterxml.jackson.databind.JsonNode
import java.math.BigInteger

data class CreateAssetSendRequest(
    val redirectUrl: String?,
    val tokenAddress: String?,
    val assetType: AssetType,
    val amount: BigInteger,
    val senderAddress: String?,
    val recipientAddress: String,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?
) {
    fun validate() = apply {
        when (assetType) {
            AssetType.NATIVE -> if (tokenAddress != null) throw TokenAddressNotAllowedException()
            AssetType.TOKEN -> if (tokenAddress == null) throw MissingTokenAddressException()
        }
    }
}
