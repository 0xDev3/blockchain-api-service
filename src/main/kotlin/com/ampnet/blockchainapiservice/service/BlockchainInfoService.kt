package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.BlockName
import com.ampnet.blockchainapiservice.util.BlockParameter
import com.ampnet.blockchainapiservice.util.ContractAddress
import java.util.UUID

interface BlockchainInfoService {
    fun fetchErc20AccountBalanceFromSignedMessage(
        messageId: UUID,
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        block: BlockParameter = BlockName.LATEST
    ): AccountBalance
}
