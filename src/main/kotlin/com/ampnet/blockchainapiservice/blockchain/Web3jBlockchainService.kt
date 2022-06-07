package com.ampnet.blockchainapiservice.blockchain

import com.ampnet.blockchainapiservice.blockchain.properties.ChainPropertiesHandler
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.exception.BlockchainReadException
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.BlockParameter
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.Erc20Balance
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.bind
import com.ampnet.blockchainapiservice.util.shortCircuiting
import mu.KLogging
import org.springframework.stereotype.Service
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.RemoteFunctionCall
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.tx.ReadonlyTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import java.io.IOException

@Service
class Web3jBlockchainService(applicationProperties: ApplicationProperties) : BlockchainService {

    companion object : KLogging()

    private val chainHandler = ChainPropertiesHandler(applicationProperties)

    override fun fetchErc20AccountBalance(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        walletAddress: WalletAddress,
        blockParameter: BlockParameter
    ): Erc20Balance {
        logger.debug {
            "Fetching ERC20 balance, chainSpec: $chainSpec, contractAddress: $contractAddress," +
                " walletAddress: $walletAddress, blockParameter: $blockParameter"
        }
        val blockchainProperties = chainHandler.getBlockchainProperties(chainSpec)
        val ethBlock = blockchainProperties.web3j.ethGetBlockByNumber(blockParameter.toWeb3Parameter(), false)
            .send().block
        val blockNumber = BlockNumber(ethBlock.number)
        val timestamp = UtcDateTime.ofEpochSeconds(ethBlock.timestamp.longValueExact())

        val contract = IERC20.load(
            contractAddress.rawValue,
            blockchainProperties.web3j,
            ReadonlyTransactionManager(blockchainProperties.web3j, contractAddress.rawValue),
            DefaultGasProvider()
        )

        contract.setDefaultBlockParameter(blockNumber.toWeb3Parameter())

        return contract.balanceOf(walletAddress.rawValue).sendSafely()
            ?.let { Erc20Balance(walletAddress, blockNumber, timestamp, Balance(it)) }
            ?: throw BlockchainReadException(
                "Unable to read ERC20 contract at address: ${contractAddress.rawValue}" +
                    " on chain ID: ${chainSpec.chainId.value}"
            )
    }

    override fun fetchTransactionInfo(chainSpec: ChainSpec, txHash: TransactionHash): BlockchainTransactionInfo? {
        logger.debug { "Fetching transaction, chainSpec: $chainSpec, txHash: $txHash" }
        val web3j = chainHandler.getBlockchainProperties(chainSpec).web3j

        return shortCircuiting {
            val transaction = web3j.ethGetTransactionByHash(txHash.value).sendSafely()
                ?.transaction?.orElse(null).bind()
            val receipt = web3j.ethGetTransactionReceipt(txHash.value).sendSafely()
                ?.transactionReceipt?.orElse(null).bind()
            val currentBlockNumber = web3j.ethBlockNumber()?.sendSafely()
                ?.blockNumber.bind()
            val blockConfirmations = currentBlockNumber - transaction.blockNumber.bind()
            val timestamp = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(transaction.blockNumber), false)
                .sendSafely()?.block?.timestamp?.let { UtcDateTime.ofEpochSeconds(it.longValueExact()) }.bind()

            BlockchainTransactionInfo(
                hash = TransactionHash(transaction.hash),
                from = WalletAddress(transaction.from),
                to = WalletAddress(transaction.to),
                data = FunctionData(transaction.input),
                blockConfirmations = blockConfirmations,
                timestamp = timestamp,
                success = receipt.isStatusOK
            )
        }
    }

    @Suppress("ReturnCount")
    fun <S, T : Response<*>?> Request<S, T>.sendSafely(): T? {
        try {
            val value = this.send()
            if (value?.hasError() == true) {
                logger.warn { "Web3j call errors: ${value.error.message}" }
                return null
            }
            return value
        } catch (ex: IOException) {
            logger.warn("Failed blockchain call", ex)
            return null
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun <T> RemoteFunctionCall<T>.sendSafely(): T? =
        try {
            this.send()
        } catch (ex: Exception) {
            logger.warn("Failed smart contract call", ex)
            null
        }
}
