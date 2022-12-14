package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentTransactionCacheId
import dev3.blockchainapiservice.generated.jooq.id.FetchAccountBalanceCacheId
import dev3.blockchainapiservice.generated.jooq.id.FetchErc20AccountBalanceCacheId
import dev3.blockchainapiservice.generated.jooq.id.FetchTransactionInfoCacheId
import dev3.blockchainapiservice.model.EventLog
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.model.result.ContractDeploymentTransactionInfo
import dev3.blockchainapiservice.util.AccountBalance
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress

interface Web3jBlockchainServiceCacheRepository {
    fun cacheFetchAccountBalance(id: FetchAccountBalanceCacheId, chainSpec: ChainSpec, accountBalance: AccountBalance)
    fun cacheFetchErc20AccountBalance(
        id: FetchErc20AccountBalanceCacheId,
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        accountBalance: AccountBalance
    )

    @Suppress("LongParameterList")
    fun cacheFetchTransactionInfo(
        id: FetchTransactionInfoCacheId,
        chainSpec: ChainSpec,
        txHash: TransactionHash,
        blockNumber: BlockNumber,
        txInfo: BlockchainTransactionInfo,
        eventLogs: List<EventLog>
    )

    fun cacheContractDeploymentTransaction(
        id: ContractDeploymentTransactionCacheId,
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        contractDeploymentTransactionInfo: ContractDeploymentTransactionInfo,
        eventLogs: List<EventLog>
    )

    fun getCachedFetchAccountBalance(
        chainSpec: ChainSpec,
        walletAddress: WalletAddress,
        blockNumber: BlockNumber
    ): AccountBalance?

    fun getCachedFetchErc20AccountBalance(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        walletAddress: WalletAddress,
        blockNumber: BlockNumber
    ): AccountBalance?

    fun getCachedFetchTransactionInfo(
        chainSpec: ChainSpec,
        txHash: TransactionHash,
        currentBlockNumber: BlockNumber
    ): Pair<BlockchainTransactionInfo, List<EventLog>>?

    fun getCachedContractDeploymentTransaction(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
    ): Pair<ContractDeploymentTransactionInfo, List<EventLog>>?
}
