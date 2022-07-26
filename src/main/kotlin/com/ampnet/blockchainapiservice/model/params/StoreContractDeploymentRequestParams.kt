package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID

data class StoreContractDeploymentRequestParams(
    val id: UUID,
    val alias: String,
    val contractId: ContractId,
    val contractData: ContractBinaryData,
    val constructorParams: JsonNode,
    val deployerAddress: WalletAddress?,
    val initialEthAmount: Balance,
    val chainId: ChainId,
    val redirectUrl: String,
    val projectId: UUID,
    val createdAt: UtcDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
) {
    companion object : ParamsFactory<PreStoreContractDeploymentRequestParams, StoreContractDeploymentRequestParams> {
        private const val PATH = "/request-deploy/\${id}/action"
        private val objectMapper = ObjectMapper()

        override fun fromCreateParams(
            id: UUID,
            params: PreStoreContractDeploymentRequestParams,
            project: Project,
            createdAt: UtcDateTime
        ) = StoreContractDeploymentRequestParams(
            id = id,
            alias = params.createParams.alias,
            contractId = params.createParams.contractId,
            contractData = ContractBinaryData(
                params.contractDecorator.binary.value + params.encodedConstructor.withoutPrefix
            ),
            constructorParams = objectMapper.createArrayNode().addAll(
                params.createParams.constructorParams.mapNotNull { it.rawJson }
            ),
            deployerAddress = params.createParams.deployerAddress,
            initialEthAmount = params.createParams.initialEthAmount,
            chainId = project.chainId,
            redirectUrl = project.createRedirectUrl(params.createParams.redirectUrl, id, PATH),
            projectId = project.id,
            createdAt = createdAt,
            arbitraryData = params.createParams.arbitraryData,
            screenConfig = params.createParams.screenConfig
        )
    }
}
