package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.Erc20BalanceRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.interfaces.IErc20BalanceRequestRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.Erc20BalanceRequestRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20BalanceRequest
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqErc20BalanceRequestRepository(private val dslContext: DSLContext) : Erc20BalanceRequestRepository {

    companion object : KLogging()

    override fun store(params: StoreErc20BalanceRequestParams): Erc20BalanceRequest {
        logger.info { "Store ERC20 balance request, params: $params" }
        val record = Erc20BalanceRequestRecord(
            id = params.id,
            projectId = params.projectId,
            chainId = params.chainId,
            redirectUrl = params.redirectUrl,
            tokenAddress = params.tokenAddress,
            blockNumber = params.blockNumber,
            requestedWalletAddress = params.requestedWalletAddress,
            arbitraryData = params.arbitraryData,
            screenBeforeActionMessage = params.screenConfig.beforeActionMessage,
            screenAfterActionMessage = params.screenConfig.afterActionMessage,
            actualWalletAddress = null,
            signedMessage = null,
            createdAt = params.createdAt
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

    override fun getAllByProjectId(projectId: UUID): List<Erc20BalanceRequest> {
        logger.debug { "Get ERC20 balance requests filtered by projectId: $projectId" }
        return dslContext.selectFrom(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST)
            .where(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.PROJECT_ID.eq(projectId))
            .orderBy(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun setSignedMessage(id: UUID, walletAddress: WalletAddress, signedMessage: SignedMessage): Boolean {
        logger.info {
            "Set walletAddress and signedMessage for ERC20 balance request, id: $id, walletAddress: $walletAddress," +
                " signedMessage: $signedMessage"
        }
        return dslContext.update(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST)
            .set(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ACTUAL_WALLET_ADDRESS, walletAddress)
            .set(Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.SIGNED_MESSAGE, signedMessage)
            .where(
                DSL.and(
                    Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ID.eq(id),
                    Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.ACTUAL_WALLET_ADDRESS.isNull(),
                    Erc20BalanceRequestTable.ERC20_BALANCE_REQUEST.SIGNED_MESSAGE.isNull()
                )
            )
            .execute() > 0
    }

    private fun IErc20BalanceRequestRecord.toModel(): Erc20BalanceRequest =
        Erc20BalanceRequest(
            id = id!!,
            projectId = projectId!!,
            chainId = chainId!!,
            redirectUrl = redirectUrl!!,
            tokenAddress = tokenAddress!!,
            blockNumber = blockNumber,
            requestedWalletAddress = requestedWalletAddress,
            actualWalletAddress = actualWalletAddress,
            signedMessage = signedMessage,
            arbitraryData = arbitraryData,
            screenConfig = ScreenConfig(
                beforeActionMessage = screenBeforeActionMessage,
                afterActionMessage = screenAfterActionMessage
            ),
            createdAt = createdAt!!
        )
}
