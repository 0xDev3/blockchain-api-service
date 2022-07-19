package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class StoreContractDeploymentRequestParams(
    val id: UUID,
    val alias: String,
    val contractId: ContractId,
    val contractData: ContractBinaryData,
    val contractTags: List<ContractTag>,
    val contractImplements: List<ContractTrait>,
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
            contractTags = params.contractDecorator.tags,
            contractImplements = params.contractDecorator.implements,
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
