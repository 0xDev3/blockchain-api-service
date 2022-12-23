package dev3.blockchainapiservice.features.contract.arbitrarycall.model.result

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.FunctionData
import dev3.blockchainapiservice.util.Status
import dev3.blockchainapiservice.util.TransactionData
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.UtcDateTime
import dev3.blockchainapiservice.util.WalletAddress
import dev3.blockchainapiservice.util.WithTransactionData
import java.util.UUID

data class ContractArbitraryCallRequest(
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
    val callerAddress: WalletAddress?,
    val txHash: TransactionHash?
) {
    fun withTransactionData(
        status: Status,
        transactionInfo: BlockchainTransactionInfo?
    ): WithTransactionData<ContractArbitraryCallRequest> =
        WithTransactionData(
            value = this,
            status = status,
            transactionData = TransactionData(
                txHash = this.txHash,
                transactionInfo = transactionInfo,
                fromAddress = this.callerAddress,
                toAddress = this.contractAddress,
                data = this.functionData,
                value = ethAmount
            )
        )
}
