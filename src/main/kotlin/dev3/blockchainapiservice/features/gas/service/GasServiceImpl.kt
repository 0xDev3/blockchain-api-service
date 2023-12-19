package dev3.blockchainapiservice.features.gas.service

import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.features.gas.model.request.EstimateGasCostForContractArbitraryCallRequest
import dev3.blockchainapiservice.model.params.DeployedContractIdentifier
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.service.DeployedContractIdentifierResolverService
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.GasEstimate
import dev3.blockchainapiservice.util.GasPrice
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.ZeroAddress
import mu.KLogging
import org.springframework.stereotype.Service

@Service
@Suppress("LongParameterList")
class GasServiceImpl(
    private val deployedContractIdentifierResolverService: DeployedContractIdentifierResolverService,
    private val blockchainService: BlockchainService
) : GasService { // TODO write integ tests

    companion object : KLogging()

    override fun estimateGasCost(
        request: EstimateGasCostForContractArbitraryCallRequest,
        project: Project
    ): GasEstimate {
        logger.debug { "Estimating gas cost, request: $request, project: $project" }

        val (_, contractAddress) = deployedContractIdentifierResolverService
            .resolveContractIdAndAddress(DeployedContractIdentifier(request), project)

        return blockchainService.estimateGas(
            chainSpec = ChainSpec(
                chainId = project.chainId,
                customRpcUrl = project.customRpcUrl
            ),
            from = WalletAddress(request.callerAddress ?: ZeroAddress.rawValue),
            to = contractAddress,
            value = Balance(request.ethAmount),
            data = FunctionData(request.functionData)
        )
    }

    override fun getGasPrice(project: Project): GasPrice {
        logger.debug { "Fetching gas price, project: $project" }
        return blockchainService.getGasPrice(
            ChainSpec(
                chainId = project.chainId,
                customRpcUrl = project.customRpcUrl
            )
        )
    }
}
