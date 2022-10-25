package dev3.blockchainapiservice.repository

import dev3.blockchainapiservice.generated.jooq.tables.AssetBalanceRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.records.AssetBalanceRequestRecord
import dev3.blockchainapiservice.model.ScreenConfig
import dev3.blockchainapiservice.model.params.StoreAssetBalanceRequestParams
import dev3.blockchainapiservice.model.result.AssetBalanceRequest
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.WalletAddress
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
        return dslContext.selectFrom(AssetBalanceRequestTable)
            .where(AssetBalanceRequestTable.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(projectId: UUID): List<AssetBalanceRequest> {
        logger.debug { "Get asset balance requests filtered by projectId: $projectId" }
        return dslContext.selectFrom(AssetBalanceRequestTable)
            .where(AssetBalanceRequestTable.PROJECT_ID.eq(projectId))
            .orderBy(AssetBalanceRequestTable.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun setSignedMessage(id: UUID, walletAddress: WalletAddress, signedMessage: SignedMessage): Boolean {
        logger.info {
            "Set walletAddress and signedMessage for asset balance request, id: $id, walletAddress: $walletAddress," +
                " signedMessage: $signedMessage"
        }
        return dslContext.update(AssetBalanceRequestTable)
            .set(AssetBalanceRequestTable.ACTUAL_WALLET_ADDRESS, walletAddress)
            .set(AssetBalanceRequestTable.SIGNED_MESSAGE, signedMessage)
            .where(
                DSL.and(
                    AssetBalanceRequestTable.ID.eq(id),
                    AssetBalanceRequestTable.ACTUAL_WALLET_ADDRESS.isNull(),
                    AssetBalanceRequestTable.SIGNED_MESSAGE.isNull()
                )
            )
            .execute() > 0
    }

    private fun AssetBalanceRequestRecord.toModel(): AssetBalanceRequest =
        AssetBalanceRequest(
            id = id,
            projectId = projectId,
            chainId = chainId,
            redirectUrl = redirectUrl,
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
            createdAt = createdAt
        )
}
