package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.BlockchainService
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.model.params.CreateReadonlyFunctionCallParams
import com.ampnet.blockchainapiservice.model.params.ExecuteReadonlyFunctionCallParams
import com.ampnet.blockchainapiservice.model.params.OutputParameter
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.model.result.ReadonlyFunctionCallResult
import com.ampnet.blockchainapiservice.util.BlockName
import mu.KLogging
import org.web3j.abi.TypeReference

class ContractReadonlyFunctionCallServiceImpl(
    private val functionEncoderService: FunctionEncoderService,
    private val deployedContractIdentifierResolverService: DeployedContractIdentifierResolverService,
    private val blockchainService: BlockchainService
) : ContractReadonlyFunctionCallService {

    companion object : KLogging()

    override fun callReadonlyContractFunction(
        params: CreateReadonlyFunctionCallParams,
        project: Project
    ): ReadonlyFunctionCallResult {
        logger.info { "Calling contract read-only function, params: $params, project: $project" }

        val (_, contractAddress) = deployedContractIdentifierResolverService
            .resolveContractIdAndAddress(params.identifier, project.id)
        val data = functionEncoderService.encode(
            functionName = params.functionName,
            arguments = params.functionParams
        )

        return blockchainService.callReadonlyFunction(
            chainSpec = ChainSpec(
                chainId = project.chainId,
                customRpcUrl = project.customRpcUrl
            ),
            params = ExecuteReadonlyFunctionCallParams(
                contractAddress = contractAddress,
                callerAddress = params.callerAddress,
                functionName = params.functionName,
                functionData = data,
                // TODO do this when decoding from JSON
                outputParameters = params.outputParameters.map {
                    OutputParameter(
                        solidityType = it,
                        typeReference = TypeReference.makeTypeReference(it)
                    )
                }
            ),
            blockParameter = params.blockNumber ?: BlockName.LATEST
        )
    }
}
