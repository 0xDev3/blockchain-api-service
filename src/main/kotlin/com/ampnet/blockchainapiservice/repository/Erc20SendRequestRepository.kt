package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.model.params.StoreErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20SendRequest
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import java.util.UUID

interface Erc20SendRequestRepository {
    fun store(params: StoreErc20SendRequestParams): Erc20SendRequest
    fun getById(id: UUID): Erc20SendRequest?
    fun getBySender(sender: WalletAddress): List<Erc20SendRequest>
    fun getByRecipient(recipient: WalletAddress): List<Erc20SendRequest>
    fun setTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress): Boolean
}
