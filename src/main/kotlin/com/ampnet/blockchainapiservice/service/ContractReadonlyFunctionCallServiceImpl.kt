package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.model.params.CreateReadonlyFunctionCallParams
import com.ampnet.blockchainapiservice.model.params.ExecuteReadonlyFunctionCallParams
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.model.result.ReadonlyFunctionCallResult
import com.ampnet.blockchainapiservice.util.BlockName
import com.ampnet.blockchainapiservice.util.WithDeployedContractIdAndAddress
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
            .resolveContractIdAndAddress(params.identifier, project.id)
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
