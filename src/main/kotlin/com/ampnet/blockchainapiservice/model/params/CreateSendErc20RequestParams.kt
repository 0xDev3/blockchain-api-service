package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode

data class CreateSendErc20RequestParams(
    val clientId: String?,
    val chainId: ChainId?,
    val redirectUrl: String?,
    val tokenAddress: ContractAddress,
    val tokenAmount: Balance,
    val tokenSenderAddress: WalletAddress?,
    val tokenRecipientAddress: WalletAddress,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
)
