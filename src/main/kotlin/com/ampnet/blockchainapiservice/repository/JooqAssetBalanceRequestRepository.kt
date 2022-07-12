package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.AssetBalanceRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.interfaces.IAssetBalanceRequestRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.AssetBalanceRequestRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreAssetBalanceRequestParams
import com.ampnet.blockchainapiservice.model.result.AssetBalanceRequest
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqAssetBalanceRequestRepository(private val dslContext: DSLContext) : AssetBalanceRequestRepository {

    companion object : KLogging()

    override fun store(params: StoreAssetBalanceRequestParams): AssetBalanceRequest {
        logger.info { "Store asset balance request, params: $params" }
        val record = AssetBalanceRequestRecord(
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

    override fun getById(id: UUID): AssetBalanceRequest? {
        logger.debug { "Get asset balance request by id: $id" }
        return dslContext.selectFrom(AssetBalanceRequestTable.ASSET_BALANCE_REQUEST)
            .where(AssetBalanceRequestTable.ASSET_BALANCE_REQUEST.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(projectId: UUID): List<AssetBalanceRequest> {
        logger.debug { "Get asset balance requests filtered by projectId: $projectId" }
        return dslContext.selectFrom(AssetBalanceRequestTable.ASSET_BALANCE_REQUEST)
            .where(AssetBalanceRequestTable.ASSET_BALANCE_REQUEST.PROJECT_ID.eq(projectId))
            .orderBy(AssetBalanceRequestTable.ASSET_BALANCE_REQUEST.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun setSignedMessage(id: UUID, walletAddress: WalletAddress, signedMessage: SignedMessage): Boolean {
        logger.info {
            "Set walletAddress and signedMessage for asset balance request, id: $id, walletAddress: $walletAddress," +
                " signedMessage: $signedMessage"
        }
        return dslContext.update(AssetBalanceRequestTable.ASSET_BALANCE_REQUEST)
            .set(AssetBalanceRequestTable.ASSET_BALANCE_REQUEST.ACTUAL_WALLET_ADDRESS, walletAddress)
            .set(AssetBalanceRequestTable.ASSET_BALANCE_REQUEST.SIGNED_MESSAGE, signedMessage)
            .where(
                DSL.and(
                    AssetBalanceRequestTable.ASSET_BALANCE_REQUEST.ID.eq(id),
                    AssetBalanceRequestTable.ASSET_BALANCE_REQUEST.ACTUAL_WALLET_ADDRESS.isNull(),
                    AssetBalanceRequestTable.ASSET_BALANCE_REQUEST.SIGNED_MESSAGE.isNull()
                )
            )
            .execute() > 0
    }

    private fun IAssetBalanceRequestRecord.toModel(): AssetBalanceRequest =
        AssetBalanceRequest(
            id = id!!,
            projectId = projectId!!,
            chainId = chainId!!,
            redirectUrl = redirectUrl!!,
            tokenAddress = tokenAddress,
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