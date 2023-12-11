package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.model.params.CreateReadonlyFunctionCallParams
import dev3.blockchainapiservice.model.params.ExecuteReadonlyFunctionCallParams
import dev3.blockchainapiservice.model.params.FunctionCallData
import dev3.blockchainapiservice.model.params.FunctionNameAndParams
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.model.result.ReadonlyFunctionCallResult
import dev3.blockchainapiservice.util.BlockName
import dev3.blockchainapiservice.util.WithDeployedContractIdAndAddress
import mu.KLogging
import org.springframework.stereotype.Service

@Service
class ContractReadonlyFunctionCallServiceImpl(
    private val functionEncoderService: FunctionEncoderService,
    private val deployedContractIdentifierResolverService: DeployedContractIdentifierResolverService,
    private val blockchainService: BlockchainService
) : ContractReadonlyFunctionCallService {

    companion object : KLogging() {
        private const val UNKNOWN_FUNCTION = "<unknown function name>"
    }

    override fun callReadonlyContractFunction(
        params: CreateReadonlyFunctionCallParams,
        project: Project
    ): WithDeployedContractIdAndAddress<ReadonlyFunctionCallResult> {
        logger.info { "Calling contract read-only function, params: $params, project: $project" }

        val (deployedContractId, contractAddress) = deployedContractIdentifierResolverService
            .resolveContractIdAndAddress(params.identifier, project)

        val (data, functionName) = when (params.functionCallInfo) {
            is FunctionCallData -> params.functionCallInfo.callData to UNKNOWN_FUNCTION
            is FunctionNameAndParams ->
                functionEncoderService.encode(
                    functionName = params.functionCallInfo.functionName,
                    arguments = params.functionCallInfo.functionParams
                ) to params.functionCallInfo.functionName
        }

        val value = blockchainService.callReadonlyFunction(
            chainSpec = ChainSpec(
                chainId = project.chainId,
                customRpcUrl = project.customRpcUrl
            ),
            params = ExecuteReadonlyFunctionCallParams(
                contractAddress = contractAddress,
                callerAddress = params.callerAddress,
                functionName = functionName,
                functionData = data,
                outputParams = params.outputParams
            ),
            blockParameter = params.blockNumber ?: BlockName.LATEST
        )

        return WithDeployedContractIdAndAddress(
            value = value,
            deployedContractId = deployedContractId,
            contractAddress = contractAddress
        )
    }
}
