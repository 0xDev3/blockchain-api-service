package dev3.blockchainapiservice.features.gas.model.response

import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.GasEstimate
import java.math.BigInteger

data class EstimateGasCostResponse(
    val chainId: Long,
    val gasEstimate: BigInteger
) {
    companion object {
        fun create(chainId: ChainId, gasEstimate: GasEstimate) = EstimateGasCostResponse(
            chainId = chainId.value,
            gasEstimate = gasEstimate.value
        )
    }
}
