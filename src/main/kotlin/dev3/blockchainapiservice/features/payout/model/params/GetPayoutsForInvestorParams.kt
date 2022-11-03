package dev3.blockchainapiservice.features.payout.model.params

import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.WalletAddress

data class GetPayoutsForInvestorParams(
    val payoutService: ContractAddress,
    val payoutManager: ContractAddress,
    val investor: WalletAddress
)
