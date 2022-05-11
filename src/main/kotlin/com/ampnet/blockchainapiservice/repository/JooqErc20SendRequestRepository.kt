package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.Erc20SendRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.interfaces.IErc20SendRequestRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.Erc20SendRequestRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20SendRequest
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
class JooqErc20SendRequestRepository(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper
) : Erc20SendRequestRepository {

    companion object : KLogging()

    override fun store(params: StoreErc20SendRequestParams): Erc20SendRequest {
        logger.info { "Store ERC20 send request, params: $params" }
        val record = Erc20SendRequestRecord(
            id = params.id,
            chainId = params.chainId.value,
            redirectUrl = params.redirectUrl,
            tokenAddress = params.tokenAddress.rawValue,
            tokenAmount = params.tokenAmount.rawValue,
            tokenSenderAddress = params.tokenSenderAddress?.rawValue,
            tokenRecipientAddress = params.tokenRecipientAddress.rawValue,
            arbitraryData = params.arbitraryData?.let { JSON.valueOf(objectMapper.writeValueAsString(it)) },
            screenBeforeActionMessage = params.screenConfig.beforeActionMessage,
            screenAfterActionMessage = params.screenConfig.afterActionMessage,
            txHash = null
        )
        dslContext.executeInsert(record)
        return record.toModel()
    }

    override fun getById(id: UUID): Erc20SendRequest? {
        logger.debug { "Get ERC20 send request by id: $id" }
        return dslContext.selectFrom(Erc20SendRequestTable.ERC20_SEND_REQUEST)
            .where(Erc20SendRequestTable.ERC20_SEND_REQUEST.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun setTxHash(id: UUID, txHash: TransactionHash): Boolean {
        logger.info { "Set txHash for ERC20 send request, id: $id, txHash: $txHash" }
        return dslContext.update(Erc20SendRequestTable.ERC20_SEND_REQUEST)
            .set(Erc20SendRequestTable.ERC20_SEND_REQUEST.TX_HASH, txHash.value)
            .where(
                DSL.and(
                    Erc20SendRequestTable.ERC20_SEND_REQUEST.ID.eq(id),
                    Erc20SendRequestTable.ERC20_SEND_REQUEST.TX_HASH.isNull()
                )
            )
            .execute() > 0
    }

    private fun IErc20SendRequestRecord.toModel(): Erc20SendRequest =
        Erc20SendRequest(
            id = id!!,
            chainId = ChainId(chainId!!),
            redirectUrl = redirectUrl!!,
            tokenAddress = ContractAddress(tokenAddress!!),
            tokenAmount = Balance(tokenAmount!!),
            tokenSenderAddress = tokenSenderAddress?.let { WalletAddress(it) },
            tokenRecipientAddress = WalletAddress(tokenRecipientAddress!!),
            txHash = txHash?.let { TransactionHash(it) },
            arbitraryData = arbitraryData?.let { objectMapper.readTree(it.data()) },
            screenConfig = ScreenConfig(
                beforeActionMessage = screenBeforeActionMessage,
                afterActionMessage = screenAfterActionMessage
            )
        )
}
