package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.AuthorizationRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.AuthorizationRequestRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreAuthorizationRequestParams
import com.ampnet.blockchainapiservice.model.result.AuthorizationRequest
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqAuthorizationRequestRepository(private val dslContext: DSLContext) : AuthorizationRequestRepository {

    companion object : KLogging() {
        private val TABLE = AuthorizationRequestTable.AUTHORIZATION_REQUEST
    }

    override fun store(params: StoreAuthorizationRequestParams): AuthorizationRequest {
        logger.info { "Store authorization request, params: $params" }
        val record = AuthorizationRequestRecord(
            id = params.id,
            projectId = params.projectId,
            redirectUrl = params.redirectUrl,
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

    override fun getById(id: UUID): AuthorizationRequest? {
        logger.debug { "Get authorization request by id: $id" }
        return dslContext.selectFrom(TABLE)
            .where(TABLE.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(projectId: UUID): List<AuthorizationRequest> {
        logger.debug { "Get authorization requests filtered by projectId: $projectId" }
        return dslContext.selectFrom(TABLE)
            .where(TABLE.PROJECT_ID.eq(projectId))
            .orderBy(TABLE.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun setSignedMessage(id: UUID, walletAddress: WalletAddress, signedMessage: SignedMessage): Boolean {
        logger.info {
            "Set walletAddress and signedMessage for authorization request, id: $id, walletAddress: $walletAddress," +
                " signedMessage: $signedMessage"
        }
        return dslContext.update(TABLE)
            .set(TABLE.ACTUAL_WALLET_ADDRESS, walletAddress)
            .set(TABLE.SIGNED_MESSAGE, signedMessage)
            .where(
                DSL.and(
                    TABLE.ID.eq(id),
                    TABLE.ACTUAL_WALLET_ADDRESS.isNull(),
                    TABLE.SIGNED_MESSAGE.isNull()
                )
            )
            .execute() > 0
    }

    private fun AuthorizationRequestRecord.toModel(): AuthorizationRequest =
        AuthorizationRequest(
            id = id,
            projectId = projectId,
            redirectUrl = redirectUrl,
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
