package dev3.blockchainapiservice.features.payout.util

import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.WalletAddress
import org.web3j.abi.TypeEncoder

data class PayoutAccountBalance(val address: WalletAddress, val balance: Balance) {
    fun abiEncode(): String = TypeEncoder.encode(address.value) + TypeEncoder.encode(balance.value)
}
