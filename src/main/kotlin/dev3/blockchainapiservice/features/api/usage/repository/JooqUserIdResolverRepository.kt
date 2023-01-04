package dev3.blockchainapiservice.features.api.usage.repository

import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import dev3.blockchainapiservice.generated.jooq.id.AssetBalanceRequestId
import dev3.blockchainapiservice.generated.jooq.id.AssetMultiSendRequestId
import dev3.blockchainapiservice.generated.jooq.id.AssetSendRequestId
import dev3.blockchainapiservice.generated.jooq.id.AuthorizationRequestId
import dev3.blockchainapiservice.generated.jooq.id.ContractArbitraryCallRequestId
import dev3.blockchainapiservice.generated.jooq.id.ContractDeploymentRequestId
import dev3.blockchainapiservice.generated.jooq.id.ContractFunctionCallRequestId
import dev3.blockchainapiservice.generated.jooq.id.DatabaseId
import dev3.blockchainapiservice.generated.jooq.id.Erc20LockRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.generated.jooq.tables.AssetBalanceRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.AssetMultiSendRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.AssetSendRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.AuthorizationRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.ContractArbitraryCallRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.ContractDeploymentRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.ContractFunctionCallRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.Erc20LockRequestTable
import dev3.blockchainapiservice.generated.jooq.tables.ProjectTable
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.TableField
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqUserIdResolverRepository(private val dslContext: DSLContext) : UserIdResolverRepository {

    companion object : KLogging()

    override fun getByProjectId(projectId: ProjectId): UserId? = getUserId(IdType.PROJECT_ID, projectId.value)

    // we want compiler type-safety of exhaustive matching of enum elements, so there is no way to reduce complexity
    @Suppress("ComplexMethod")
    override fun getUserId(idType: IdType, id: UUID): UserId? {
        logger.debug { "Resolving project ID, idType: $idType, id: $id" }

        val projectId = when (idType) {
            IdType.PROJECT_ID ->
                ProjectTable.run { ID.select(ProjectId(id), ID) }

            IdType.ASSET_BALANCE_REQUEST_ID ->
                AssetBalanceRequestTable.run { ID.select(AssetBalanceRequestId(id), PROJECT_ID) }

            IdType.ASSET_MULTI_SEND_REQUEST_ID ->
                AssetMultiSendRequestTable.run { ID.select(AssetMultiSendRequestId(id), PROJECT_ID) }

            IdType.ASSET_SEND_REQUEST_ID ->
                AssetSendRequestTable.run { ID.select(AssetSendRequestId(id), PROJECT_ID) }

            IdType.AUTHORIZATION_REQUEST_ID ->
                AuthorizationRequestTable.run { ID.select(AuthorizationRequestId(id), PROJECT_ID) }

            IdType.CONTRACT_DEPLOYMENT_REQUEST_ID ->
                ContractDeploymentRequestTable.run { ID.select(ContractDeploymentRequestId(id), PROJECT_ID) }

            IdType.FUNCTION_CALL_REQUEST_ID ->
                ContractFunctionCallRequestTable.run { ID.select(ContractFunctionCallRequestId(id), PROJECT_ID) }

            IdType.ARBITRARY_CALL_REQUEST_ID ->
                ContractArbitraryCallRequestTable.run { ID.select(ContractArbitraryCallRequestId(id), PROJECT_ID) }

            IdType.ERC20_LOCK_REQUEST_ID ->
                Erc20LockRequestTable.run { ID.select(Erc20LockRequestId(id), PROJECT_ID) }
        }

        return projectId?.let {
            logger.debug { "Project user ID, projectId: $it" }
            dslContext.select(ProjectTable.OWNER_ID)
                .from(ProjectTable)
                .where(ProjectTable.ID.eq(it))
                .fetchOne(ProjectTable.OWNER_ID)
        }
    }

    private fun <R : Record, I : DatabaseId> TableField<R, I>.select(
        id: I,
        projectIdField: TableField<*, ProjectId>
    ): ProjectId? =
        dslContext.select(projectIdField)
            .from(table)
            .where(this.eq(id))
            .fetchOne(projectIdField)
}
