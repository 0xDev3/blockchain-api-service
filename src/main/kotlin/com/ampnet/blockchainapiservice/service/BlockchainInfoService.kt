package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.BlockName
import com.ampnet.blockchainapiservice.util.BlockParameter
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import java.util.UUID

interface BlockchainInfoService {
    fun fetchErc20AccountBalanceFromSignedMessage(
        messageId: UUID,
        chainId: ChainId,
        contractAddress: ContractAddress,
        block: BlockParameter = BlockName.LATEST
    ): AccountBalance
}
