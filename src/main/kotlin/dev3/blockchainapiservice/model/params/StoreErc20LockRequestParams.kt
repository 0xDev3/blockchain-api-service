package dev3.blockchainapiservice.model.params

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.generated.jooq.id.Erc20LockRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.DurationSeconds
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import java.util.UUID

data class StoreErc20LockRequestParams(
    val id: Erc20LockRequestId,
    val projectId: ProjectId,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress,
    val tokenAmount: Balance,
    val lockDuration: DurationSeconds,
    val lockContractAddress: ContractAddress,
    val tokenSenderAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val createdAt: UtcDateTime
) {
    companion object : ParamsFactory<CreateErc20LockRequestParams, StoreErc20LockRequestParams> {
        private const val PATH = "/request-lock/\${id}/action"

        override fun fromCreateParams(
            id: UUID,
            params: CreateErc20LockRequestParams,
            project: Project,
            createdAt: UtcDateTime
        ) = StoreErc20LockRequestParams(
            id = Erc20LockRequestId(id),
            projectId = project.id,
            chainId = project.chainId,
            redirectUrl = project.createRedirectUrl(params.redirectUrl, id, PATH),
            tokenAddress = params.tokenAddress,
            tokenAmount = params.tokenAmount,
            lockDuration = params.lockDuration,
            lockContractAddress = params.lockContractAddress,
            tokenSenderAddress = params.tokenSenderAddress,
            arbitraryData = params.arbitraryData,
            screenConfig = params.screenConfig,
            createdAt = createdAt
        )
    }
}
