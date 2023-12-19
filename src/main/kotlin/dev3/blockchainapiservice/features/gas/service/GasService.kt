package dev3.blockchainapiservice.features.gas.service

import dev3.blockchainapiservice.features.gas.model.request.EstimateGasCostForContractArbitraryCallRequest
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.GasEstimate
import dev3.blockchainapiservice.util.GasPrice

interface GasService {
    fun estimateGasCost(
        request: EstimateGasCostForContractArbitraryCallRequest,
        project: Project
    ): GasEstimate

    fun getGasPrice(project: Project): GasPrice
}
