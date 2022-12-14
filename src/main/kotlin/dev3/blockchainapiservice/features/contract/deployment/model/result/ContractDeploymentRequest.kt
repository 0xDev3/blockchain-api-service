package dev3.blockchainapiservice.features.contract.deployment.model.result

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.ContractBinaryData
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.ContractTag
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.InterfaceId
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.TransactionData
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithTransactionData
import dev3.blockchainapiservice.util.ZeroAddress

data class ContractDeploymentRequest(
    val id: ContractDeploymentRequestId,
    val alias: String,
    val name: String?,
    val description: String?,
    val contractId: ContractId,
    val contractData: ContractBinaryData,
    val constructorParams: JsonNode,
    val contractTags: List<ContractTag>,
    val contractImplements: List<InterfaceId>,
    val initialEthAmount: Balance,
    val chainId: ChainId,
    val redirectUrl: String,
    val projectId: ProjectId,
    val createdAt: UtcDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val contractAddress: ContractAddress?,
    val deployerAddress: WalletAddress?,
    val txHash: TransactionHash?,
    val imported: Boolean,
    val proxy: Boolean,
    val implementationContractAddress: ContractAddress?
) {
    fun withTransactionData(
        status: Status,
        transactionInfo: BlockchainTransactionInfo?
    ): WithTransactionData<ContractDeploymentRequest> =
        WithTransactionData(
            value = this,
            status = status,
            transactionData = TransactionData(
                txHash = this.txHash,
                transactionInfo = transactionInfo,
                fromAddress = this.deployerAddress,
                toAddress = ZeroAddress,
                data = FunctionData(contractData.value),
                value = initialEthAmount
            )
        )
}
