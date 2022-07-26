package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID
import com.ampnet.blockchainapiservice.model.params.PreStoreContractFunctionCallRequestParams as PreStoreParams

data class StoreContractFunctionCallRequestParams(
    val id: UUID,
    val deployedContractId: UUID?,
    val contractAddress: ContractAddress,
    val functionName: String,
    val functionParams: JsonNode,
    val ethAmount: Balance,
    val chainId: ChainId,
    val redirectUrl: String,
    val projectId: UUID,
    val createdAt: UtcDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val callerAddress: WalletAddress?
) {
    companion object : ParamsFactory<PreStoreParams, StoreContractFunctionCallRequestParams> {
        private const val PATH = "/request-function-call/\${id}/action"
        private val objectMapper = ObjectMapper()

        override fun fromCreateParams(
            id: UUID,
            params: PreStoreParams,
            project: Project,
            createdAt: UtcDateTime
        ) = StoreContractFunctionCallRequestParams(
            id = id,
            deployedContractId = params.deployedContractId,
            contractAddress = params.contractAddress,
            functionName = params.createParams.functionName,
            functionParams = objectMapper.createArrayNode().addAll(
                params.createParams.functionParams.mapNotNull { it.rawJson }
            ),
            ethAmount = params.createParams.ethAmount,
            chainId = project.chainId,
            redirectUrl = project.createRedirectUrl(params.createParams.redirectUrl, id, PATH),
            projectId = project.id,
            createdAt = createdAt,
            arbitraryData = params.createParams.arbitraryData,
            screenConfig = params.createParams.screenConfig,
            callerAddress = params.createParams.callerAddress
        )
    }
}
