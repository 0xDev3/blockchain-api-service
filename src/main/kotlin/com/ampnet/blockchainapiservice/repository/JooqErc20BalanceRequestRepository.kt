package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.Erc20BalanceRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.Erc20BalanceRequestRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20BalanceRequest
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqErc20BalanceRequestRepository(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper
) : Erc20BalanceRequestRepository {

    companion object : KLogging()

    override fun store(params: StoreErc20BalanceRequestParams): Erc20BalanceRequest {
        logger.info { "Store ERC20 balance request, params: $params" }
        val record = Erc20BalanceRequestRecord(
            id = params.id,
            chainId = params.chainId.value,
            redirectUrl = params.redirectUrl,
            tokenAddress = params.tokenAddress.rawValue,
            blockNumber = params.blockNumber?.value,
            requestedWalletAddress = params.requestedWalletAddress?.rawValue,
            arbitraryData = params.arbitraryData?.let { JSON.valueOf(objectMapper.writeValueAsString(it)) },
            balanceScreenBeforeActionMessage = params.screenConfig.beforeActionMessage,
            balanceScreenAfterActionMessage = params.screenConfig.afterActionMessage,
            actualWalletAddress = null,
            signedMessage = null
        )
        dslContext.executeInsert(record)
        return record.toModel()
    }

    override fun getById(id: UUID): Erc20BalanceRequest? {
        logger.debug { "Get ERC20 balance request by id: $id" }
        return dslContext.selectFrom(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST)
            .where(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun setSignedMessage(id: UUID, walletAddress: WalletAddress, signedMessage: SignedMessage): Boolean {
        logger.info {
            "Set walletAddress and signedMessage for ERC20 balance request, id: $id, walletAddress: $walletAddress," +
                " signedMessage: $signedMessage"
        }
        return dslContext.update(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST)
            .set(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ACTUAL_WALLET_ADDRESS, walletAddress.rawValue)
            .set(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.SIGNED_MESSAGE, signedMessage.value)
            .where(
                DSL.and(
                    Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ID.eq(id),
                    Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ACTUAL_WALLET_ADDRESS.isNull(),
                    Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.SIGNED_MESSAGE.isNull()
                )
            )
            .execute() > 0
    }

    private fun Erc20BalanceRequestRecord.toModel(): Erc20BalanceRequest =
        Erc20BalanceRequest(
            id = id!!,
            chainId = ChainId(chainId!!),
            redirectUrl = redirectUrl!!,
            tokenAddress = ContractAddress(tokenAddress!!),
            blockNumber = blockNumber?.let { BlockNumber(it) },
            requestedWalletAddress = requestedWalletAddress?.let { WalletAddress(it) },
            actualWalletAddress = actualWalletAddress?.let { WalletAddress(it) },
            signedMessage = signedMessage?.let { SignedMessage(it) },
            arbitraryData = arbitraryData?.let { objectMapper.readTree(it.data()) },
            screenConfig = ScreenConfig(
                beforeActionMessage = balanceScreenBeforeActionMessage,
                afterActionMessage = balanceScreenAfterActionMessage
            )
        )
}
