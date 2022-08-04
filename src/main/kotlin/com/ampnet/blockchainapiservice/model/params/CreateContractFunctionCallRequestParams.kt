package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.exception.InvalidRequestBodyException
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.request.CreateContractFunctionCallRequest
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode

data class CreateContractFunctionCallRequestParams(
    val identifier: DeployedContractIdentifier,
    val functionName: String,
    val functionParams: List<FunctionArgument>,
    val ethAmount: Balance,
    val redirectUrl: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val callerAddress: WalletAddress?
) {
    companion object {
        operator fun invoke(requestBody: CreateContractFunctionCallRequest): CreateContractFunctionCallRequestParams {
            val identifier = listOfNotNull(
                requestBody.deployedContractId?.let { DeployedContractIdIdentifier(it) },
                requestBody.deployedContractAlias?.let { DeployedContractAliasIdentifier(it) },
                requestBody.contractAddress?.let { DeployedContractAddressIdentifier(ContractAddress(it)) }
            )
                .takeIf { it.size == 1 }
                ?.firstOrNull()
                ?: throw InvalidRequestBodyException(
                    "Exactly one of the possible contract identifier values must be specified:" +
                        " [deployed_contract_id, deployed_contract_address, contract_address]"
                )

            return CreateContractFunctionCallRequestParams(
                identifier = identifier,
                functionName = requestBody.functionName,
                functionParams = requestBody.functionParams,
                ethAmount = Balance(requestBody.ethAmount),
                redirectUrl = requestBody.redirectUrl,
                arbitraryData = requestBody.arbitraryData,
                screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY,
                callerAddress = requestBody.callerAddress?.let { WalletAddress(it) }
            )
        }
    }
}
