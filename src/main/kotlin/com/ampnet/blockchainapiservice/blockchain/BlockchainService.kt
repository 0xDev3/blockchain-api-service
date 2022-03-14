package com.ampnet.blockchainapiservice.blockchain

import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.BlockName
import com.ampnet.blockchainapiservice.util.BlockParameter
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress

interface BlockchainService {
    fun fetchErc20AccountBalance(
        chainId: ChainId,
        contractAddress: ContractAddress,
        walletAddress: WalletAddress,
        block: BlockParameter = BlockName.LATEST
    ): AccountBalance
}
