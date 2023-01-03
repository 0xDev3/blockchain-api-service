package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.generated.jooq.id.AssetSendRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.tables.AssetSendRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.records.AssetSendRequestRecord
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.params.StoreAssetSendRequestParams
import dev3.blockchainapiservice.model.result.AssetSendRequest
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.coalesce
import org.springframework.stereotype.Repository

@Repository
class JooqAssetSendRequestRepository(private val dslContext: DSLContext) : AssetSendRequestRepository {

    companion object : KLogging()

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

    override fun getById(id: AssetSendRequestId): AssetSendRequest? {
        logger.debug { "Get asset send request by id: $id" }
        return dslContext.selectFrom(AssetSendRequestTable)
            .where(AssetSendRequestTable.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(projectId: ProjectId): List<AssetSendRequest> {
        logger.debug { "Get asset send requests filtered by projectId: $projectId" }
        return dslContext.selectFrom(AssetSendRequestTable)
            .where(AssetSendRequestTable.PROJECT_ID.eq(projectId))
            .orderBy(AssetSendRequestTable.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun getBySender(sender: WalletAddress): List<AssetSendRequest> {
        logger.debug { "Get asset send requests filtered by sender address: $sender" }
        return dslContext.selectFrom(AssetSendRequestTable)
            .where(AssetSendRequestTable.ASSET_SENDER_ADDRESS.eq(sender))
            .orderBy(AssetSendRequestTable.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun getByRecipient(recipient: WalletAddress): List<AssetSendRequest> {
        logger.debug { "Get asset send requests filtered by recipient address: $recipient" }
        return dslContext.selectFrom(AssetSendRequestTable)
            .where(AssetSendRequestTable.ASSET_RECIPIENT_ADDRESS.eq(recipient))
            .orderBy(AssetSendRequestTable.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun setTxInfo(id: AssetSendRequestId, txHash: TransactionHash, caller: WalletAddress): Boolean {
        logger.info { "Set txInfo for asset send request, id: $id, txHash: $txHash, caller: $caller" }
        return dslContext.update(AssetSendRequestTable)
            .set(AssetSendRequestTable.TX_HASH, txHash)
            .set(
                AssetSendRequestTable.ASSET_SENDER_ADDRESS,
                coalesce(AssetSendRequestTable.ASSET_SENDER_ADDRESS, caller)
            )
            .where(
                DSL.and(
                    AssetSendRequestTable.ID.eq(id),
                    AssetSendRequestTable.TX_HASH.isNull()
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
