package dev3.blockchainapiservice.features.contract.arbitrarycall.model.params

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.params.ParamsFactory
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID
import dev3.blockchainapiservice.features.contract.arbitrarycall.model.params.PreStoreContractArbitraryCallRequestParams as PreStoreParams

data class StoreContractArbitraryCallRequestParams(
    val id: UUID,
    val deployedContractId: UUID?,
    val contractAddress: ContractAddress,
    val functionData: FunctionData,
    val functionName: String?,
    val functionParams: JsonNode?,
    val ethAmount: Balance,
    val chainId: ChainId,
    val redirectUrl: String,
    val projectId: UUID,
    val createdAt: UtcDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val callerAddress: WalletAddress?
) {
    companion object : ParamsFactory<PreStoreParams, StoreContractArbitraryCallRequestParams> {
        private const val PATH = "/request-arbitrary-call/\${id}/action"

        override fun fromCreateParams(
            id: UUID,
            params: PreStoreParams,
            project: Project,
            createdAt: UtcDateTime
        ) = StoreContractArbitraryCallRequestParams(
            id = id,
            deployedContractId = params.deployedContractId,
            contractAddress = params.contractAddress,
            functionData = params.createParams.functionData,
            functionName = params.functionName,
            functionParams = params.functionParams,
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
