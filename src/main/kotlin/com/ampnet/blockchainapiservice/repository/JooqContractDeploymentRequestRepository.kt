package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.exception.AliasAlreadyInUseException
import com.ampnet.blockchainapiservice.generated.jooq.tables.ContractDeploymentRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ContractMetadataTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ContractDeploymentRequestRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.filters.AndList
import com.ampnet.blockchainapiservice.model.filters.ContractDeploymentRequestFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.params.StoreContractDeploymentRequestParams
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentRequest
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.DSL.coalesce
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Repository
import java.util.UUID

@Suppress("TooManyFunctions")
@Repository
class JooqContractDeploymentRequestRepository(
    private val dslContext: DSLContext
) : ContractDeploymentRequestRepository {

    companion object : KLogging()

    override fun store(params: StoreContractDeploymentRequestParams): ContractDeploymentRequest {
        logger.info { "Store contract deployment request, params: $params" }
        val record = ContractDeploymentRequestRecord(
            id = params.id,
            alias = params.alias,
            contractMetadataId = null,
            contractData = params.contractData,
            initialEthAmount = params.initialEthAmount,
            chainId = params.chainId,
            redirectUrl = params.redirectUrl,
            projectId = params.projectId,
            createdAt = params.createdAt,
            arbitraryData = params.arbitraryData,
            screenBeforeActionMessage = params.screenConfig.beforeActionMessage,
            screenAfterActionMessage = params.screenConfig.afterActionMessage,
            contractAddress = null,
            deployerAddress = params.deployerAddress,
            txHash = null
        )
        val selectContractMetadataId = DSL.select(ContractMetadataTable.CONTRACT_METADATA.ID)
            .from(ContractMetadataTable.CONTRACT_METADATA)
            .where(ContractMetadataTable.CONTRACT_METADATA.CONTRACT_ID.eq(params.contractId))

        try {
            dslContext.insertInto(ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST)
                .set(record)
                .set(
                    ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST.CONTRACT_METADATA_ID,
                    selectContractMetadataId
                ).execute()
        } catch (e: DuplicateKeyException) {
            throw AliasAlreadyInUseException(params.alias)
        }

        return getById(params.id)!!
    }

    override fun getById(id: UUID): ContractDeploymentRequest? {
        logger.debug { "Get contract deployment request by id: $id" }
        return dslContext.selectWithJoin()
            .where(ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(
        projectId: UUID,
        filters: ContractDeploymentRequestFilters
    ): List<ContractDeploymentRequest> {
        logger.debug { "Get contract deployment requests by projectId: $projectId, filters: $filters" }

        val conditions = listOfNotNull(
            ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST.PROJECT_ID.eq(projectId),
            filters.contractIds.orCondition(),
            filters.contractTags.orAndCondition { it.contractTagsAndCondition() },
            filters.contractImplements.orAndCondition { it.contractTraitsAndCondition() },
            filters.deployedOnly.takeIf { it }?.let {
                ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST.TX_HASH.isNotNull()
            }
        )

        return dslContext.selectWithJoin()
            .where(conditions)
            .orderBy(ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun setTxInfo(id: UUID, txHash: TransactionHash, deployer: WalletAddress): Boolean {
        logger.info { "Set txInfo for contract deployment request, id: $id, txHash: $txHash, deployer: $deployer" }
        return dslContext.update(ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST)
            .set(ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST.TX_HASH, txHash)
            .set(
                ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST.DEPLOYER_ADDRESS,
                coalesce(ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST.DEPLOYER_ADDRESS, deployer)
            )
            .where(
                DSL.and(
                    ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST.ID.eq(id),
                    ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST.TX_HASH.isNull()
                )
            )
            .execute() > 0
    }

    override fun setContractAddress(id: UUID, contractAddress: ContractAddress): Boolean {
        logger.info {
            "Set contract address for contract deployment request, id: $id, contractAddress: $contractAddress"
        }
        return dslContext.update(ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST)
            .set(ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST.CONTRACT_ADDRESS, contractAddress)
            .where(
                DSL.and(
                    ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST.ID.eq(id),
                    ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST.CONTRACT_ADDRESS.isNull()
                )
            )
            .execute() > 0
    }

    private fun Record.toModel(): ContractDeploymentRequest {
        val requestRecord = this.into(ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST)
        val metadataRecord = this.into(ContractMetadataTable.CONTRACT_METADATA)

        return ContractDeploymentRequest(
            id = requestRecord.id!!,
            alias = requestRecord.alias!!,
            contractId = metadataRecord.contractId!!,
            contractData = requestRecord.contractData!!,
            contractTags = metadataRecord.contractTags!!.map { ContractTag(it!!) },
            contractImplements = metadataRecord.contractImplements!!.map { ContractTrait(it!!) },
            initialEthAmount = requestRecord.initialEthAmount!!,
            chainId = requestRecord.chainId!!,
            redirectUrl = requestRecord.redirectUrl!!,
            projectId = requestRecord.projectId!!,
            createdAt = requestRecord.createdAt!!,
            arbitraryData = requestRecord.arbitraryData,
            screenConfig = ScreenConfig(
                beforeActionMessage = requestRecord.screenBeforeActionMessage,
                afterActionMessage = requestRecord.screenAfterActionMessage
            ),
            contractAddress = requestRecord.contractAddress,
            deployerAddress = requestRecord.deployerAddress,
            txHash = requestRecord.txHash
        )
    }

    private fun DSLContext.selectWithJoin() = select()
        .from(ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST)
        .join(ContractMetadataTable.CONTRACT_METADATA)
        .on(
            ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST.CONTRACT_METADATA_ID
                .eq(ContractMetadataTable.CONTRACT_METADATA.ID)
        )

    private fun OrList<ContractId>.orCondition(): Condition? =
        takeIf { list.isNotEmpty() }?.let {
            ContractMetadataTable.CONTRACT_METADATA.CONTRACT_ID.`in`(it.list)
        }

    private fun AndList<ContractTag>.contractTagsAndCondition(): Condition? =
        takeIf { list.isNotEmpty() }?.let {
            ContractMetadataTable.CONTRACT_METADATA.CONTRACT_TAGS.contains(
                it.list.map { v -> v.value }.toTypedArray()
            )
        }

    private fun AndList<ContractTrait>.contractTraitsAndCondition(): Condition? =
        takeIf { list.isNotEmpty() }?.let {
            ContractMetadataTable.CONTRACT_METADATA.CONTRACT_IMPLEMENTS.contains(
                it.list.map { v -> v.value }.toTypedArray()
            )
        }

    private fun <T> OrList<AndList<T>>.orAndCondition(innerConditionMapping: (AndList<T>) -> Condition?): Condition? =
        list.mapNotNull(innerConditionMapping).takeIf { it.isNotEmpty() }?.let { DSL.or(it) }
}
