package dev3.blockchainapiservice.blockchain

import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.model.params.ExecuteReadonlyFunctionCallParams
import dev3.blockchainapiservice.model.result.BlockchainTransactionInfo
import dev3.blockchainapiservice.model.result.ContractDeploymentTransactionInfo
import dev3.blockchainapiservice.model.result.ReadonlyFunctionCallResult
import dev3.blockchainapiservice.util.AccountBalance
import dev3.blockchainapiservice.util.BlockName
import dev3.blockchainapiservice.util.BlockParameter
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress

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
