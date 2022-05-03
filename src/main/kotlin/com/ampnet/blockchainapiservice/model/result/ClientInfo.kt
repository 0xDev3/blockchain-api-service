package com.ampnet.blockchainapiservice.model.result

import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.Resolvable

data class ClientInfo(
    val clientId: String,
    val chainId: Resolvable<ChainId>,
    val sendRedirectUrl: Resolvable<String>,
    val balanceRedirectUrl: Resolvable<String>,
    val tokenAddress: Resolvable<ContractAddress>
) {
    companion object {
        val EMPTY = ClientInfo("", null, null, null, null)
    }

    constructor(
        clientId: String,
        chainId: ChainId?,
        sendRedirectUrl: String?,
        balanceRedirectUrl: String?,
        tokenAddress: ContractAddress?
    ) : this(
        clientId = clientId,
        chainId = Resolvable(chainId, "Missing chainId"),
        sendRedirectUrl = Resolvable(sendRedirectUrl, "Missing redirectUrl"),
        balanceRedirectUrl = Resolvable(balanceRedirectUrl, "Missing redirectUrl"),
        tokenAddress = Resolvable(tokenAddress, "Missing tokenAddress")
    )
}
