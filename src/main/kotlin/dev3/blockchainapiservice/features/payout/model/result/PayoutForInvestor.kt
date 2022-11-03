package dev3.blockchainapiservice.features.payout.model.result

import dev3.blockchainapiservice.blockchain.PayoutStateForInvestor
import dev3.blockchainapiservice.blockchain.PayoutStruct
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.WalletAddress

data class PayoutForInvestor(
    val payout: Payout,
    val investor: WalletAddress,
    val amountClaimed: Balance
) {
    constructor(struct: PayoutStruct, state: PayoutStateForInvestor) : this(
        payout = Payout(struct),
        investor = WalletAddress(state.investor),
        amountClaimed = Balance(state.amountClaimed)
    )
}
