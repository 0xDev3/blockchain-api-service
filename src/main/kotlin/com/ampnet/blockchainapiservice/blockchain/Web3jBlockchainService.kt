package com.ampnet.blockchainapiservice.blockchain

import com.ampnet.blockchainapiservice.blockchain.properties.ChainPropertiesHandler
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.exception.BlockchainReadException
import com.ampnet.blockchainapiservice.exception.TemporaryBlockchainReadException
import com.ampnet.blockchainapiservice.model.params.ExecuteReadonlyFunctionCallParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ReadonlyFunctionCallResult
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.BlockParameter
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.ZeroAddress
import com.ampnet.blockchainapiservice.util.bind
import com.ampnet.blockchainapiservice.util.shortCircuiting
import mu.KLogging
import org.springframework.stereotype.Service
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Type
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.RemoteFunctionCall
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.tx.ReadonlyTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import java.io.IOException

@Service
class Web3jBlockchainService(applicationProperties: ApplicationProperties) : BlockchainService {

    companion object : KLogging()

    private val chainHandler = ChainPropertiesHandler(applicationProperties)

    override fun fetchAccountBalance(
        chainSpec: ChainSpec,
        walletAddress: WalletAddress,
        blockParameter: BlockParameter
    ): AccountBalance {
        logger.debug {
            "Fetching account balance, chainSpec: $chainSpec, walletAddress: $walletAddress," +
                " blockParameter: $blockParameter"
        }
        val blockchainProperties = chainHandler.getBlockchainProperties(chainSpec)
        val (blockNumber, timestamp) = blockchainProperties.web3j.getBlockNumberAndTimestamp(blockParameter)
        val balance = blockchainProperties.web3j.ethGetBalance(walletAddress.rawValue, blockNumber.toWeb3Parameter())
            .sendSafely()?.balance?.let { Balance(it) }
            ?: throw BlockchainReadException("Unable to read balance of address: ${walletAddress.rawValue}")

        return AccountBalance(walletAddress, blockNumber, timestamp, balance)
    }

    override fun fetchErc20AccountBalance(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        walletAddress: WalletAddress,
        blockParameter: BlockParameter
    ): AccountBalance {
        logger.debug {
            "Fetching ERC20 balance, chainSpec: $chainSpec, contractAddress: $contractAddress," +
                " walletAddress: $walletAddress, blockParameter: $blockParameter"
        }
        val blockchainProperties = chainHandler.getBlockchainProperties(chainSpec)
        val (blockNumber, timestamp) = blockchainProperties.web3j.getBlockNumberAndTimestamp(blockParameter)

        val contract = IERC20.load(
            contractAddress.rawValue,
            blockchainProperties.web3j,
            ReadonlyTransactionManager(blockchainProperties.web3j, contractAddress.rawValue),
            DefaultGasProvider()
        )

        contract.setDefaultBlockParameter(blockNumber.toWeb3Parameter())

        return contract.balanceOf(walletAddress.rawValue).sendSafely()
            ?.let { AccountBalance(walletAddress, blockNumber, timestamp, Balance(it)) }
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
                to = transaction.to?.let { WalletAddress(it) } ?: ZeroAddress.toWalletAddress(),
                deployedContractAddress = receipt.contractAddress?.let { ContractAddress(it) },
                data = FunctionData(transaction.input),
                value = Balance(transaction.value),
                blockConfirmations = blockConfirmations,
                timestamp = timestamp,
                success = receipt.isStatusOK
            )
        }
    }

    override fun callReadonlyFunction(
        chainSpec: ChainSpec,
        params: ExecuteReadonlyFunctionCallParams,
        blockParameter: BlockParameter
    ): ReadonlyFunctionCallResult {
        logger.debug {
            "Executing read-only function call, chainSpec: $chainSpec, params: $params, blockParameter: $blockParameter"
        }
        val blockchainProperties = chainHandler.getBlockchainProperties(chainSpec)
        val (blockNumber, timestamp) = blockchainProperties.web3j.getBlockNumberAndTimestamp(blockParameter)
        val functionCallResponse = blockchainProperties.web3j.ethCall(
            Transaction.createEthCallTransaction(
                params.callerAddress.rawValue,
                params.contractAddress.rawValue,
                params.functionData.value
            ),
            blockNumber.toWeb3Parameter()
        ).sendSafely()?.value?.takeIf { it != "0x" }
            ?: throw BlockchainReadException(
                "Unable to call function ${params.functionName} on contract with address: ${params.contractAddress}"
            )
        // TODO test with various return values to make sure everything works as intended...
        @Suppress("UNCHECKED_CAST") val returnValues = FunctionReturnDecoder.decode(
            functionCallResponse,
            params.outputParameters.map { it.typeReference } as List<TypeReference<Type<*>>>
        )
            .map { it.value }

        return ReadonlyFunctionCallResult(
            blockNumber = blockNumber,
            timestamp = timestamp,
            returnValues = returnValues
        )
    }

    @Suppress("ReturnCount")
    fun Web3j.getBlockNumberAndTimestamp(blockParameter: BlockParameter): Pair<BlockNumber, UtcDateTime> {
        val block = ethGetBlockByNumber(blockParameter.toWeb3Parameter(), false).sendSafely()?.block
        val blockNumber = block?.number?.let { BlockNumber(it) }
        val timestamp = block?.timestamp?.let { UtcDateTime.ofEpochSeconds(it.longValueExact()) }
        return blockNumber?.let { b -> timestamp?.let { t -> Pair(b, t) } } ?: throw TemporaryBlockchainReadException()
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
