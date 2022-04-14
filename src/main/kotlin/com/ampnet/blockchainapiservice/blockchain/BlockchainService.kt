package com.ampnet.blockchainapiservice.blockchain

import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.BlockName
import com.ampnet.blockchainapiservice.util.BlockParameter
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress

interface BlockchainService {
    fun fetchErc20AccountBalance(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        walletAddress: WalletAddress,
        block: BlockParameter = BlockName.LATEST
    ): AccountBalance

    fun fetchTransaction(chainSpec: ChainSpec, txHash: TransactionHash): BlockchainTransactionInfo?
}
