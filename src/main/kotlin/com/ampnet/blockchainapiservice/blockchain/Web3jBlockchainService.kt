package com.ampnet.blockchainapiservice.blockchain

import com.ampnet.blockchainapiservice.blockchain.properties.ChainPropertiesHandler
import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.exception.BlockchainReadException
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BlockParameter
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.springframework.stereotype.Service
import org.web3j.protocol.core.RemoteFunctionCall
import org.web3j.tx.ReadonlyTransactionManager
import org.web3j.tx.gas.DefaultGasProvider

@Service
class Web3jBlockchainService(applicationProperties: ApplicationProperties) : BlockchainService {

    companion object : KLogging()

    private val chainHandler = ChainPropertiesHandler(applicationProperties)

    override fun fetchErc20AccountBalance(
        chainId: ChainId,
        contractAddress: ContractAddress,
        walletAddress: WalletAddress,
        block: BlockParameter
    ): AccountBalance {
        logger.debug {
            "Fetching ERC20 balance, chainId: $chainId, contractAddress: $contractAddress," +
                " walletAddress: $walletAddress, block: $block"
        }
        val blockchainProperties = chainHandler.getBlockchainProperties(chainId)
        val contract = IERC20.load(
            contractAddress.rawValue,
            blockchainProperties.web3j,
            ReadonlyTransactionManager(blockchainProperties.web3j, contractAddress.rawValue),
            DefaultGasProvider()
        )

        contract.setDefaultBlockParameter(block.toWeb3Parameter())

        return contract.balanceOf(walletAddress.rawValue).sendSafely()
            ?.let { AccountBalance(walletAddress, Balance(it)) }
            ?: throw BlockchainReadException(
                "Unable to read ERC20 contract at address: ${contractAddress.rawValue} on chain ID: ${chainId.value}"
            )
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
