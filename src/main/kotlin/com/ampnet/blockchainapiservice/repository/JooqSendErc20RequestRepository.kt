package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.SendErc20RequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.SendErc20RequestRecord
import com.ampnet.blockchainapiservice.model.SendScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreSendErc20RequestParams
import com.ampnet.blockchainapiservice.model.result.SendErc20Request
import com.ampnet.blockchainapiservice.model.result.TransactionData
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqSendErc20RequestRepository(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper
) : SendErc20RequestRepository {

    companion object : KLogging()

    override fun store(params: StoreSendErc20RequestParams): SendErc20Request {
        logger.info { "Store send ERC20 request, params: $params" }
        val record = SendErc20RequestRecord(
            id = params.id,
            chainId = params.chainId.value,
            redirectUrl = params.redirectUrl,
            tokenAddress = params.tokenAddress.rawValue,
            amount = params.amount.rawValue,
            fromAddress = params.fromAddress?.rawValue,
            toAddress = params.toAddress.rawValue,
            arbitraryData = params.arbitraryData?.let { JSON.valueOf(objectMapper.writeValueAsString(it)) },
            sendScreenTitle = params.screenConfig.title,
            sendScreenMessage = params.screenConfig.message,
            sendScreenLogo = params.screenConfig.logo,
            txHash = null
        )
        dslContext.executeInsert(record)
        return record.toModel()
    }

    override fun getById(id: UUID): SendErc20Request? {
        logger.debug { "Get send ERC20 request by id: $id" }
        return dslContext.selectFrom(SendErc20RequestTable.SEND_ERC20_REQUEST)
            .where(SendErc20RequestTable.SEND_ERC20_REQUEST.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun setTxHash(id: UUID, txHash: TransactionHash): Boolean {
        logger.info { "Set txHash for send ERC20 request, id: $id, txHash: $txHash" }
        return dslContext.update(SendErc20RequestTable.SEND_ERC20_REQUEST)
            .set(SendErc20RequestTable.SEND_ERC20_REQUEST.TX_HASH, txHash.value)
            .where(
                DSL.and(
                    SendErc20RequestTable.SEND_ERC20_REQUEST.ID.eq(id),
                    SendErc20RequestTable.SEND_ERC20_REQUEST.TX_HASH.isNull()
                )
            )
            .execute() > 0
    }

    private fun SendErc20RequestRecord.toModel(): SendErc20Request =
        SendErc20Request(
            id = id!!,
            chainId = ChainId(chainId!!),
            redirectUrl = redirectUrl!!,
            tokenAddress = ContractAddress(tokenAddress!!),
            amount = Balance(amount!!),
            arbitraryData = arbitraryData?.let { objectMapper.readTree(it.data()) },
            sendScreenConfig = SendScreenConfig(
                title = sendScreenTitle,
                message = sendScreenMessage,
                logo = sendScreenLogo
            ),
            transactionData = TransactionData(
                txHash = txHash?.let { TransactionHash(it) },
                fromAddress = fromAddress?.let { WalletAddress(it) },
                toAddress = WalletAddress(toAddress!!)
            )
        )
}
