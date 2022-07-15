package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.ContractDeploymentRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.interfaces.IContractDeploymentRequestRecord
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
import org.jooq.impl.DSL
import org.jooq.impl.DSL.coalesce
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqContractDeploymentRequestRepository(
    private val dslContext: DSLContext
) : ContractDeploymentRequestRepository {

    companion object : KLogging()

    override fun store(params: StoreContractDeploymentRequestParams): ContractDeploymentRequest {
        logger.info { "Store contract deployment request, params: $params" }
        val record = ContractDeploymentRequestRecord(
            id = params.id,
            contractId = params.contractId,
            contractData = params.contractData,
            contractTags = params.contractTags.map { it.value }.toTypedArray(),
            contractImplements = params.contractImplements.map { it.value }.toTypedArray(),
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
        dslContext.executeInsert(record)
        return record.toModel()
    }

    override fun getById(id: UUID): ContractDeploymentRequest? {
        logger.debug { "Get contract deployment request by id: $id" }
        return dslContext.selectFrom(ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST)
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

        return dslContext.selectFrom(ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST)
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

    private fun IContractDeploymentRequestRecord.toModel(): ContractDeploymentRequest =
        ContractDeploymentRequest(
            id = id!!,
            contractId = contractId!!,
            contractData = contractData!!,
            contractTags = contractTags!!.map { ContractTag(it!!) },
            contractImplements = contractImplements!!.map { ContractTrait(it!!) },
            initialEthAmount = initialEthAmount!!,
            chainId = chainId!!,
            redirectUrl = redirectUrl!!,
            projectId = projectId!!,
            createdAt = createdAt!!,
            arbitraryData = arbitraryData,
            screenConfig = ScreenConfig(
                beforeActionMessage = screenBeforeActionMessage,
                afterActionMessage = screenAfterActionMessage
            ),
            contractAddress = contractAddress,
            deployerAddress = deployerAddress,
            txHash = txHash
        )

    private fun OrList<ContractId>.orCondition(): Condition? =
        takeIf { list.isNotEmpty() }?.let {
            ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST.CONTRACT_ID.`in`(it.list)
        }

    private fun AndList<ContractTag>.contractTagsAndCondition(): Condition? =
        takeIf { list.isNotEmpty() }?.let {
            ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST.CONTRACT_TAGS.contains(
                it.list.map { v -> v.value }.toTypedArray()
            )
        }

    private fun AndList<ContractTrait>.contractTraitsAndCondition(): Condition? =
        takeIf { list.isNotEmpty() }?.let {
            ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST.CONTRACT_IMPLEMENTS.contains(
                it.list.map { v -> v.value }.toTypedArray()
            )
        }

    private fun <T> OrList<AndList<T>>.orAndCondition(innerConditionMapping: (AndList<T>) -> Condition?): Condition? =
        list.mapNotNull(innerConditionMapping).takeIf { it.isNotEmpty() }?.let { DSL.or(it) }
}
