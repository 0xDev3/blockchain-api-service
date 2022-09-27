package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.AssetSendRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.AssetSendRequestRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreAssetSendRequestParams
import com.ampnet.blockchainapiservice.model.result.AssetSendRequest
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.coalesce
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqAssetSendRequestRepository(private val dslContext: DSLContext) : AssetSendRequestRepository {

    companion object : KLogging() {
        private val TABLE = AssetSendRequestTable.ASSET_SEND_REQUEST
    }

    override fun store(params: StoreAssetSendRequestParams): AssetSendRequest {
        logger.info { "Store asset send request, params: $params" }
        val record = AssetSendRequestRecord(
            id = params.id,
            projectId = params.projectId,
            chainId = params.chainId,
            redirectUrl = params.redirectUrl,
            tokenAddress = params.tokenAddress,
            assetAmount = params.assetAmount,
            assetSenderAddress = params.assetSenderAddress,
            assetRecipientAddress = params.assetRecipientAddress,
            arbitraryData = params.arbitraryData,
            screenBeforeActionMessage = params.screenConfig.beforeActionMessage,
            screenAfterActionMessage = params.screenConfig.afterActionMessage,
            txHash = null,
            createdAt = params.createdAt
        )
        dslContext.executeInsert(record)
        return record.toModel()
    }

    override fun getById(id: UUID): AssetSendRequest? {
        logger.debug { "Get asset send request by id: $id" }
        return dslContext.selectFrom(TABLE)
            .where(TABLE.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(projectId: UUID): List<AssetSendRequest> {
        logger.debug { "Get asset send requests filtered by projectId: $projectId" }
        return dslContext.selectFrom(TABLE)
            .where(TABLE.PROJECT_ID.eq(projectId))
            .orderBy(TABLE.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun getBySender(sender: WalletAddress): List<AssetSendRequest> {
        logger.debug { "Get asset send requests filtered by sender address: $sender" }
        return dslContext.selectFrom(TABLE)
            .where(TABLE.ASSET_SENDER_ADDRESS.eq(sender))
            .orderBy(TABLE.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun getByRecipient(recipient: WalletAddress): List<AssetSendRequest> {
        logger.debug { "Get asset send requests filtered by recipient address: $recipient" }
        return dslContext.selectFrom(TABLE)
            .where(TABLE.ASSET_RECIPIENT_ADDRESS.eq(recipient))
            .orderBy(TABLE.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun setTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress): Boolean {
        logger.info { "Set txInfo for asset send request, id: $id, txHash: $txHash, caller: $caller" }
        return dslContext.update(TABLE)
            .set(TABLE.TX_HASH, txHash)
            .set(
                TABLE.ASSET_SENDER_ADDRESS,
                coalesce(TABLE.ASSET_SENDER_ADDRESS, caller)
            )
            .where(
                DSL.and(
                    TABLE.ID.eq(id),
                    TABLE.TX_HASH.isNull()
                )
            )
            .execute() > 0
    }

    private fun AssetSendRequestRecord.toModel(): AssetSendRequest =
        AssetSendRequest(
            id = id,
            projectId = projectId,
            chainId = chainId,
            redirectUrl = redirectUrl,
            tokenAddress = tokenAddress,
            assetAmount = assetAmount,
            assetSenderAddress = assetSenderAddress,
            assetRecipientAddress = assetRecipientAddress,
            txHash = txHash,
            arbitraryData = arbitraryData,
            screenConfig = ScreenConfig(
                beforeActionMessage = screenBeforeActionMessage,
                afterActionMessage = screenAfterActionMessage
            ),
            createdAt = createdAt
        )
}
