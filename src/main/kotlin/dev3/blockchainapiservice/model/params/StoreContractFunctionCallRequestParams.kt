package dev3.blockchainapiservice.model.params

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.generated.jooq.id.ContractFunctionCallRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID
import dev3.blockchainapiservice.model.params.PreStoreContractFunctionCallRequestParams as PreStoreParams

data class StoreContractFunctionCallRequestParams(
    val id: ContractFunctionCallRequestId,
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: ContractAddress,
    val functionName: String,
    val functionParams: JsonNode,
    val ethAmount: Balance,
    val chainId: ChainId,
    val redirectUrl: String,
    val projectId: ProjectId,
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
            id = ContractFunctionCallRequestId(id),
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
