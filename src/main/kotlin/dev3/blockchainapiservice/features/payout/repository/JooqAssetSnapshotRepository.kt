package dev3.blockchainapiservice.features.payout.repository

import dev3.blockchainapiservice.features.payout.model.params.CreateAssetSnapshotParams
import dev3.blockchainapiservice.features.payout.model.result.AssetSnapshot
import dev3.blockchainapiservice.features.payout.model.result.OtherAssetSnapshotData
import dev3.blockchainapiservice.features.payout.model.result.PendingAssetSnapshot
import dev3.blockchainapiservice.features.payout.model.result.SuccessfulAssetSnapshotData
import dev3.blockchainapiservice.features.payout.util.AssetSnapshotFailureCause
import dev3.blockchainapiservice.features.payout.util.AssetSnapshotStatus
import dev3.blockchainapiservice.features.payout.util.IpfsHash
import dev3.blockchainapiservice.generated.jooq.id.AssetSnapshotId
import dev3.blockchainapiservice.generated.jooq.id.MerkleTreeRootId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.tables.AssetSnapshotTable
import dev3.blockchainapiservice.generated.jooq.tables.records.AssetSnapshotRecord
import dev3.blockchainapiservice.service.UuidProvider
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class JooqAssetSnapshotRepository(private val dslContext: DSLContext, private val uuidProvider: UuidProvider) :
    AssetSnapshotRepository {

    companion object : KLogging()

    override fun getById(assetSnapshotId: AssetSnapshotId): AssetSnapshot? {
        logger.debug { "Fetching asset snapshot, assetSnapshotId: $assetSnapshotId" }
        return dslContext.selectFrom(AssetSnapshotTable)
            .where(AssetSnapshotTable.ID.eq(assetSnapshotId))
            .fetchOne()
            ?.toModel()
    }

    override fun getAllByProjectIdAndStatuses(
        projectId: ProjectId,
        statuses: Set<AssetSnapshotStatus>
    ): List<AssetSnapshot> {
        logger.debug { "Fetching all asset snapshots for projectId: $projectId, statuses: $statuses" }

        val projectIdCondition = AssetSnapshotTable.PROJECT_ID.eq(projectId)
        val dbStatuses = statuses.map { it.toDbEnum }
        val statusesCondition = dbStatuses.takeIf { it.isNotEmpty() }?.let { AssetSnapshotTable.STATUS.`in`(it) }
        val conditions = listOfNotNull(projectIdCondition, statusesCondition)

        return dslContext.selectFrom(AssetSnapshotTable)
            .where(DSL.and(conditions))
            .fetch { it.toModel() }
    }

    override fun createAssetSnapshot(params: CreateAssetSnapshotParams): AssetSnapshotId {
        logger.info { "Storing pending asset snapshot, params: $params" }

        val assetSnapshotId = uuidProvider.getUuid(AssetSnapshotId)

        dslContext.executeInsert(
            AssetSnapshotRecord(
                id = assetSnapshotId,
                projectId = params.projectId,
                name = params.name,
                chainId = params.chainId,
                assetContractAddress = params.assetContractAddress,
                blockNumber = params.payoutBlock,
                ignoredHolderAddresses = params.ignoredHolderAddresses.map { it.rawValue }.toTypedArray(),
                status = AssetSnapshotStatus.PENDING,
                resultTree = null,
                treeIpfsHash = null,
                totalAssetAmount = null,
                failureCause = null
            )
        )

        return assetSnapshotId
    }

    override fun getPending(): PendingAssetSnapshot? {
        return dslContext.selectFrom(AssetSnapshotTable)
            .where(AssetSnapshotTable.STATUS.eq(AssetSnapshotStatus.PENDING))
            .limit(1)
            .forUpdate()
            .skipLocked()
            .fetchOne()?.let {
                PendingAssetSnapshot(
                    id = it.id,
                    projectId = it.projectId,
                    name = it.name,
                    chainId = it.chainId,
                    assetContractAddress = it.assetContractAddress,
                    blockNumber = it.blockNumber,
                    ignoredHolderAddresses = it.ignoredHolderAddresses.mapTo(HashSet()) { a -> WalletAddress(a) }
                )
            }
    }

    override fun completeAssetSnapshot(
        assetSnapshotId: AssetSnapshotId,
        merkleTreeRootId: MerkleTreeRootId,
        merkleTreeIpfsHash: IpfsHash,
        totalAssetAmount: Balance
    ): AssetSnapshot? {
        logger.info {
            "Marking asset snapshot as success, assetSnapshotId: $assetSnapshotId," +
                " merkleTreeRootId: $merkleTreeRootId, merkleTreeIpfsHash: $merkleTreeIpfsHash," +
                " totalAssetAmount: $totalAssetAmount"
        }
        return dslContext.update(AssetSnapshotTable)
            .set(AssetSnapshotTable.STATUS, AssetSnapshotStatus.SUCCESS)
            .set(AssetSnapshotTable.RESULT_TREE, merkleTreeRootId)
            .set(AssetSnapshotTable.TREE_IPFS_HASH, merkleTreeIpfsHash)
            .set(AssetSnapshotTable.TOTAL_ASSET_AMOUNT, totalAssetAmount)
            .where(AssetSnapshotTable.ID.eq(assetSnapshotId))
            .returning()
            .fetchOne()
            ?.toModel()
    }

    override fun failAssetSnapshot(assetSnapshotId: AssetSnapshotId, cause: AssetSnapshotFailureCause): AssetSnapshot? {
        logger.info { "Marking asset snapshot as failed, assetSnapshotId: $assetSnapshotId" }
        return dslContext.update(AssetSnapshotTable)
            .set(AssetSnapshotTable.STATUS, AssetSnapshotStatus.FAILED)
            .set(AssetSnapshotTable.FAILURE_CAUSE, cause)
            .where(AssetSnapshotTable.ID.eq(assetSnapshotId))
            .returning()
            .fetchOne()
            ?.toModel()
    }

    private fun AssetSnapshotRecord.toModel(): AssetSnapshot {
        val assetSnapshotData = if (status == AssetSnapshotStatus.SUCCESS) {
            SuccessfulAssetSnapshotData(
                merkleTreeRootId = resultTree!!,
                merkleTreeIpfsHash = treeIpfsHash!!,
                totalAssetAmount = totalAssetAmount!!
            )
        } else OtherAssetSnapshotData(status, failureCause)

        return AssetSnapshot(
            id = id,
            projectId = projectId,
            name = name,
            chainId = chainId,
            assetContractAddress = assetContractAddress,
            blockNumber = blockNumber,
            ignoredHolderAddresses = ignoredHolderAddresses.mapTo(HashSet()) { WalletAddress(it) },
            data = assetSnapshotData
        )
    }
}
