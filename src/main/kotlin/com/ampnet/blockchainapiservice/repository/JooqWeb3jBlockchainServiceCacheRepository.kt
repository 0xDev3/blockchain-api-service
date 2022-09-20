package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.generated.jooq.tables.FetchAccountBalanceCacheTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.FetchErc20AccountBalanceCacheTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.FetchTransactionInfoCacheTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.FetchAccountBalanceCacheRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.FetchErc20AccountBalanceCacheRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.FetchTransactionInfoCacheRecord
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
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

    companion object : KLogging() {
        private val FETCH_ACCOUNT_BALANCE_CACHE_TABLE = FetchAccountBalanceCacheTable.FETCH_ACCOUNT_BALANCE_CACHE
        private val FETCH_ERC20_ACCOUNT_BALANCE_CACHE_TABLE =
            FetchErc20AccountBalanceCacheTable.FETCH_ERC20_ACCOUNT_BALANCE_CACHE
        private val FETCH_TRANSACTION_INFO_CACHE_TABLE = FetchTransactionInfoCacheTable.FETCH_TRANSACTION_INFO_CACHE
    }

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

        return dslContext.selectFrom(FETCH_ACCOUNT_BALANCE_CACHE_TABLE)
            .where(
                DSL.and(
                    FETCH_ACCOUNT_BALANCE_CACHE_TABLE.CHAIN_ID.eq(chainSpec.chainId),
                    FETCH_ACCOUNT_BALANCE_CACHE_TABLE.CUSTOM_RPC_URL.eq(chainSpec.customRpcUrl ?: ""),
                    FETCH_ACCOUNT_BALANCE_CACHE_TABLE.WALLET_ADDRESS.eq(walletAddress),
                    FETCH_ACCOUNT_BALANCE_CACHE_TABLE.BLOCK_NUMBER.eq(blockNumber)
                )
            )
            .fetchOne()
            ?.let {
                AccountBalance(
                    wallet = it.walletAddress!!,
                    blockNumber = it.blockNumber!!,
                    timestamp = it.timestamp!!,
                    amount = it.assetAmount!!
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

        return dslContext.selectFrom(FETCH_ERC20_ACCOUNT_BALANCE_CACHE_TABLE)
            .where(
                DSL.and(
                    FETCH_ERC20_ACCOUNT_BALANCE_CACHE_TABLE.CHAIN_ID.eq(chainSpec.chainId),
                    FETCH_ERC20_ACCOUNT_BALANCE_CACHE_TABLE.CUSTOM_RPC_URL.eq(chainSpec.customRpcUrl ?: ""),
                    FETCH_ERC20_ACCOUNT_BALANCE_CACHE_TABLE.CONTRACT_ADDRESS.eq(contractAddress),
                    FETCH_ERC20_ACCOUNT_BALANCE_CACHE_TABLE.WALLET_ADDRESS.eq(walletAddress),
                    FETCH_ERC20_ACCOUNT_BALANCE_CACHE_TABLE.BLOCK_NUMBER.eq(blockNumber)
                )
            )
            .fetchOne()
            ?.let {
                AccountBalance(
                    wallet = it.walletAddress!!,
                    blockNumber = it.blockNumber!!,
                    timestamp = it.timestamp!!,
                    amount = it.assetAmount!!
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

        return dslContext.selectFrom(FETCH_TRANSACTION_INFO_CACHE_TABLE)
            .where(
                DSL.and(
                    FETCH_TRANSACTION_INFO_CACHE_TABLE.CHAIN_ID.eq(chainSpec.chainId),
                    FETCH_TRANSACTION_INFO_CACHE_TABLE.CUSTOM_RPC_URL.eq(chainSpec.customRpcUrl ?: ""),
                    FETCH_TRANSACTION_INFO_CACHE_TABLE.TX_HASH.eq(txHash)
                )
            )
            .fetchOne()
            ?.let {
                BlockchainTransactionInfo(
                    hash = it.txHash!!,
                    from = it.fromAddress!!,
                    to = it.toAddress!!,
                    deployedContractAddress = it.deployedContractAddress,
                    data = it.txData!!,
                    value = it.valueAmount!!,
                    blockConfirmations = (currentBlockNumber.value - it.blockNumber!!.value).max(BigInteger.ZERO),
                    timestamp = it.timestamp!!,
                    success = it.success!!
                )
            }
    }
}
