package com.ampnet.blockchainapiservice.blockchain

import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.model.params.ExecuteReadonlyFunctionCallParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ReadonlyFunctionCallResult
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.BlockName
import com.ampnet.blockchainapiservice.util.BlockParameter
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress

interface BlockchainService {
    fun fetchAccountBalance(
        chainSpec: ChainSpec,
        walletAddress: WalletAddress,
        blockParameter: BlockParameter = BlockName.LATEST
    ): AccountBalance

    fun fetchErc20AccountBalance(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        walletAddress: WalletAddress,
        blockParameter: BlockParameter = BlockName.LATEST
    ): AccountBalance

    fun fetchTransactionInfo(chainSpec: ChainSpec, txHash: TransactionHash): BlockchainTransactionInfo?

    fun callReadonlyFunction(
        chainSpec: ChainSpec,
        params: ExecuteReadonlyFunctionCallParams,
        blockParameter: BlockParameter = BlockName.LATEST
    ): ReadonlyFunctionCallResult

    fun findContractDeploymentTransaction(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress
    ): ContractDeploymentTransactionInfo?
}
