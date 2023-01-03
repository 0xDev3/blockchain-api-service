package dev3.blockchainapiservice.features.contract.readcall.service

import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.features.contract.readcall.model.params.CreateReadonlyFunctionCallParams
import dev3.blockchainapiservice.features.contract.readcall.model.params.ExecuteReadonlyFunctionCallParams
import dev3.blockchainapiservice.features.contract.readcall.model.result.ReadonlyFunctionCallResult
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.service.DeployedContractIdentifierResolverService
import dev3.blockchainapiservice.service.FunctionEncoderService
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

    companion object : KLogging()

    override fun callReadonlyContractFunction(
        params: CreateReadonlyFunctionCallParams,
        project: Project
    ): WithDeployedContractIdAndAddress<ReadonlyFunctionCallResult> {
        logger.info { "Calling contract read-only function, params: $params, project: $project" }

        val (deployedContractId, contractAddress) = deployedContractIdentifierResolverService
            .resolveContractIdAndAddress(params.identifier, project)
        val data = functionEncoderService.encode(
            functionName = params.functionName,
            arguments = params.functionParams
        )

        val value = blockchainService.callReadonlyFunction(
            chainSpec = ChainSpec(
                chainId = project.chainId,
                customRpcUrl = project.customRpcUrl
            ),
            params = ExecuteReadonlyFunctionCallParams(
                contractAddress = contractAddress,
                callerAddress = params.callerAddress,
                functionName = params.functionName,
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
