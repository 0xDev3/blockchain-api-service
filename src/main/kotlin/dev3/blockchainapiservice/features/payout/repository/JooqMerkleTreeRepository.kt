package dev3.blockchainapiservice.features.payout.repository

import dev3.blockchainapiservice.features.payout.model.params.FetchMerkleTreeParams
import dev3.blockchainapiservice.features.payout.model.params.FetchMerkleTreePathParams
import dev3.blockchainapiservice.features.payout.model.result.MerkleTreeWithId
import dev3.blockchainapiservice.features.payout.util.MerkleTree
import dev3.blockchainapiservice.features.payout.util.PayoutAccountBalance
import dev3.blockchainapiservice.generated.jooq.id.MerkleTreeLeafId
import dev3.blockchainapiservice.generated.jooq.id.MerkleTreeRootId
import dev3.blockchainapiservice.generated.jooq.tables.MerkleTreeLeafNodeTable
import dev3.blockchainapiservice.generated.jooq.tables.MerkleTreeRootTable
import dev3.blockchainapiservice.generated.jooq.tables.records.MerkleTreeLeafNodeRecord
import dev3.blockchainapiservice.generated.jooq.tables.records.MerkleTreeRootRecord
import dev3.blockchainapiservice.service.UuidProvider
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class JooqMerkleTreeRepository(private val dslContext: DSLContext, private val uuidProvider: UuidProvider) :
    MerkleTreeRepository {

    companion object : KLogging()

    override fun getById(treeId: MerkleTreeRootId): MerkleTree? {
        logger.debug { "Fetching Merkle tree, treeId: $treeId" }

        return dslContext.selectFrom(MerkleTreeRootTable)
            .where(MerkleTreeRootTable.ID.eq(treeId))
            .fetchOne()
            ?.let { rebuildTree(it) }
    }

    override fun storeTree(
        tree: MerkleTree,
        chainId: ChainId,
        assetContractAddress: ContractAddress,
        blockNumber: BlockNumber
    ): MerkleTreeRootId {
        logger.info {
            "Storing Merkle tree with root hash: ${tree.root.hash} for chainId: $chainId," +
                " assetContractAddress: $assetContractAddress, blockNumber: $blockNumber"
        }

        val rootId = uuidProvider.getUuid(MerkleTreeRootId)

        dslContext.executeInsert(
            MerkleTreeRootRecord(
                id = rootId,
                chainId = chainId,
                assetContractAddress = assetContractAddress,
                blockNumber = blockNumber,
                merkleHash = tree.root.hash,
                hashFn = tree.hashFn
            )
        )

        val insert = dslContext.insertQuery(MerkleTreeLeafNodeTable)

        tree.leafNodesByHash.values.forEach {
            insert.addRecord(
                MerkleTreeLeafNodeRecord(
                    id = uuidProvider.getUuid(MerkleTreeLeafId),
                    merkleRoot = rootId,
                    walletAddress = it.value.data.address,
                    assetAmount = it.value.data.balance
                )
            )
        }

        insert.execute()

        return rootId
    }

    override fun fetchTree(params: FetchMerkleTreeParams): MerkleTreeWithId? {
        logger.debug { "Fetching Merkle, params: $params" }

        val root = dslContext.selectFrom(MerkleTreeRootTable)
            .where(
                DSL.and(
                    MerkleTreeRootTable.CHAIN_ID.eq(params.chainId),
                    MerkleTreeRootTable.ASSET_CONTRACT_ADDRESS.eq(params.assetContractAddress),
                    MerkleTreeRootTable.MERKLE_HASH.eq(params.rootHash)
                )
            )
            .fetchOne() ?: return null

        val tree = rebuildTree(root)

        return if (tree.root.hash == params.rootHash) {
            logger.debug { "Successfully fetched and reconstructed Merkle tree, params: $params" }
            MerkleTreeWithId(root.id, tree)
        } else {
            logger.error { "Failed to reconstruct Merkle tree, params: $params" }
            null
        }
    }

    override fun containsAddress(params: FetchMerkleTreePathParams): Boolean {
        logger.debug { "Checking if Merkle tree contains address, params: $params" }

        val root = dslContext.selectFrom(MerkleTreeRootTable)
            .where(
                DSL.and(
                    MerkleTreeRootTable.CHAIN_ID.eq(params.chainId),
                    MerkleTreeRootTable.ASSET_CONTRACT_ADDRESS.eq(params.assetContractAddress),
                    MerkleTreeRootTable.MERKLE_HASH.eq(params.rootHash)
                )
            )
            .fetchOne() ?: return false

        return dslContext.fetchExists(
            dslContext.selectFrom(MerkleTreeLeafNodeTable)
                .where(
                    DSL.and(
                        MerkleTreeLeafNodeTable.MERKLE_ROOT.eq(root.id),
                        MerkleTreeLeafNodeTable.WALLET_ADDRESS.eq(params.walletAddress)
                    )
                )
        )
    }

    private fun rebuildTree(root: MerkleTreeRootRecord): MerkleTree {
        val leafNodes = dslContext.selectFrom(MerkleTreeLeafNodeTable)
            .where(MerkleTreeLeafNodeTable.MERKLE_ROOT.eq(root.id))
            .fetch { PayoutAccountBalance(it.walletAddress, it.assetAmount) }
        return MerkleTree(leafNodes, root.hashFn)
    }
}
