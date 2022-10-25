package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.generated.jooq.tables.FetchAccountBalanceCacheTable
import dev3.blockchainapiservice.generated.jooq.tables.FetchErc20AccountBalanceCacheTable
import dev3.blockchainapiservice.generated.jooq.tables.FetchTransactionInfoCacheTable
import dev3.blockchainapiservice.generated.jooq.tables.records.FetchAccountBalanceCacheRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.FetchErc20AccountBalanceCacheRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.FetchTransactionInfoCacheRecord
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.util.AccountBalance
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Repository
import java.math.BigInteger
import java.util.UUID

@Repository
class JooqWeb3jBlockchainServiceCacheRepository(private val dslContext: DSLContext) :
    Web3jBlockchainServiceCacheRepository {

    companion object : KLogging()

    override fun cacheFetchAccountBalance(id: UUID, chainSpec: ChainSpec, accountBalance: AccountBalance) {
        logger.info {
            "Caching fetchAccountBalance call, id: $id, chainSpec: $chainSpec, accountBalance: $accountBalance"
        }

        try {
            dslContext.executeInsert(
                FetchAccountBalanceCacheRecord(
                    id = id,
                    chainId = chainSpec.chainId,
                    customRpcUrl = chainSpec.customRpcUrl ?: "",
                    walletAddress = accountBalance.wallet,
                    blockNumber = accountBalance.blockNumber,
                    timestamp = accountBalance.timestamp,
                    assetAmount = accountBalance.amount
                )
            )
        } catch (_: DuplicateKeyException) {
            logger.info {
                "Already cached fetchAccountBalance call, id: $id, chainSpec: $chainSpec," +
                    " accountBalance: $accountBalance"
            }
        }
    }

    override fun cacheFetchErc20AccountBalance(
        id: UUID,
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        accountBalance: AccountBalance
    ) {
        logger.info {
            "Caching fetchErc20AccountBalance call, id: $id, chainSpec: $chainSpec," +
                " contractAddress: $contractAddress, accountBalance: $accountBalance"
        }

        try {
            dslContext.executeInsert(
                FetchErc20AccountBalanceCacheRecord(
                    id = id,
                    chainId = chainSpec.chainId,
                    customRpcUrl = chainSpec.customRpcUrl ?: "",
                    contractAddress = contractAddress,
                    walletAddress = accountBalance.wallet,
                    blockNumber = accountBalance.blockNumber,
                    timestamp = accountBalance.timestamp,
                    assetAmount = accountBalance.amount
                )
            )
        } catch (_: DuplicateKeyException) {
            logger.info {
                "Already cached fetchErc20AccountBalance call, id: $id, chainSpec: $chainSpec," +
                    " contractAddress: $contractAddress, accountBalance: $accountBalance"
            }
        }
    }

    override fun cacheFetchTransactionInfo(
        id: UUID,
        chainSpec: ChainSpec,
        txHash: TransactionHash,
        blockNumber: BlockNumber,
        txInfo: BlockchainTransactionInfo
    ) {
        logger.info {
            "Caching fetchTransactionInfo call, id: $id, chainSpec: $chainSpec, txHash: $txHash," +
                " blockNumber: $blockNumber"
        }

        try {
            dslContext.executeInsert(
                FetchTransactionInfoCacheRecord(
                    id = id,
                    chainId = chainSpec.chainId,
                    customRpcUrl = chainSpec.customRpcUrl ?: "",
                    txHash = txHash,
                    fromAddress = txInfo.from,
                    toAddress = txInfo.to.toWalletAddress(),
                    deployedContractAddress = txInfo.deployedContractAddress,
                    txData = txInfo.data,
                    valueAmount = txInfo.value,
                    blockNumber = blockNumber,
                    timestamp = txInfo.timestamp,
                    success = txInfo.success
                )
            )
        } catch (_: DuplicateKeyException) {
            logger.info {
                "Already cached fetchTransactionInfo call, id: $id, chainSpec: $chainSpec, txHash: $txHash," +
                    " blockNumber: $blockNumber"
            }
        }
    }

    override fun getCachedFetchAccountBalance(
        chainSpec: ChainSpec,
        walletAddress: WalletAddress,
        blockNumber: BlockNumber
    ): AccountBalance? {
        logger.debug {
            "Get cached fetchAccountBalance call, chainSpec: $chainSpec, walletAddress: $walletAddress," +
                " blockNumber: $blockNumber"
        }

        return dslContext.selectFrom(FetchAccountBalanceCacheTable)
            .where(
                DSL.and(
                    FetchAccountBalanceCacheTable.CHAIN_ID.eq(chainSpec.chainId),
                    FetchAccountBalanceCacheTable.CUSTOM_RPC_URL.eq(chainSpec.customRpcUrl ?: ""),
                    FetchAccountBalanceCacheTable.WALLET_ADDRESS.eq(walletAddress),
                    FetchAccountBalanceCacheTable.BLOCK_NUMBER.eq(blockNumber)
                )
            )
            .fetchOne()
            ?.let {
                AccountBalance(
                    wallet = it.walletAddress,
                    blockNumber = it.blockNumber,
                    timestamp = it.timestamp,
                    amount = it.assetAmount
                )
            }
    }

    override fun getCachedFetchErc20AccountBalance(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        walletAddress: WalletAddress,
        blockNumber: BlockNumber
    ): AccountBalance? {
        logger.debug {
            "Get cached fetchErc20AccountBalance call, chainSpec: $chainSpec, contractAddress: $contractAddress," +
                " walletAddress: $walletAddress, blockNumber: $blockNumber"
        }

        return dslContext.selectFrom(FetchErc20AccountBalanceCacheTable)
            .where(
                DSL.and(
                    FetchErc20AccountBalanceCacheTable.CHAIN_ID.eq(chainSpec.chainId),
                    FetchErc20AccountBalanceCacheTable.CUSTOM_RPC_URL.eq(chainSpec.customRpcUrl ?: ""),
                    FetchErc20AccountBalanceCacheTable.CONTRACT_ADDRESS.eq(contractAddress),
                    FetchErc20AccountBalanceCacheTable.WALLET_ADDRESS.eq(walletAddress),
                    FetchErc20AccountBalanceCacheTable.BLOCK_NUMBER.eq(blockNumber)
                )
            )
            .fetchOne()
            ?.let {
                AccountBalance(
                    wallet = it.walletAddress,
                    blockNumber = it.blockNumber,
                    timestamp = it.timestamp,
                    amount = it.assetAmount
                )
            }
    }

    override fun getCachedFetchTransactionInfo(
        chainSpec: ChainSpec,
        txHash: TransactionHash,
        currentBlockNumber: BlockNumber
    ): BlockchainTransactionInfo? {
        logger.debug {
            "Get cached fetchTransactionInfo call, chainSpec: $chainSpec, txHash: $txHash," +
                " currentBlockNumber: $currentBlockNumber"
        }

        return dslContext.selectFrom(FetchTransactionInfoCacheTable)
            .where(
                DSL.and(
                    FetchTransactionInfoCacheTable.CHAIN_ID.eq(chainSpec.chainId),
                    FetchTransactionInfoCacheTable.CUSTOM_RPC_URL.eq(chainSpec.customRpcUrl ?: ""),
                    FetchTransactionInfoCacheTable.TX_HASH.eq(txHash)
                )
            )
            .fetchOne()
            ?.let {
                BlockchainTransactionInfo(
                    hash = it.txHash,
                    from = it.fromAddress,
                    to = it.toAddress,
                    deployedContractAddress = it.deployedContractAddress,
                    data = it.txData,
                    value = it.valueAmount,
                    blockConfirmations = (currentBlockNumber.value - it.blockNumber.value).max(BigInteger.ZERO),
                    timestamp = it.timestamp,
                    success = it.success
                )
            }
    }
}
