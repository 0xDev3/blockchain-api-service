package dev3.blockchainapiservice.features.gas.model.response

import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.GasPrice
import java.math.BigInteger

data class GasPriceResponse(
    val chainId: Long,
    val gasPrice: BigInteger
) {
    companion object {
        fun create(chainId: ChainId, gasPrice: GasPrice) = GasPriceResponse(
            chainId = chainId.value,
            gasPrice = gasPrice.value
        )
    }
}
