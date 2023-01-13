package dev3.blockchainapiservice.features.payout.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.TestData
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.payout.model.params.CreateAssetSnapshotParams
import dev3.blockchainapiservice.features.payout.model.request.CreateAssetSnapshotRequest
import dev3.blockchainapiservice.features.payout.model.response.AssetSnapshotsResponse
import dev3.blockchainapiservice.features.payout.model.response.CreateAssetSnapshotResponse
import dev3.blockchainapiservice.features.payout.model.result.FullAssetSnapshot
import dev3.blockchainapiservice.features.payout.model.result.FullAssetSnapshotData
import dev3.blockchainapiservice.features.payout.service.AssetSnapshotQueueService
import dev3.blockchainapiservice.features.payout.util.AssetSnapshotStatus
import dev3.blockchainapiservice.features.payout.util.HashFunction
import dev3.blockchainapiservice.features.payout.util.IpfsHash
import dev3.blockchainapiservice.features.payout.util.MerkleHash
import dev3.blockchainapiservice.generated.jooq.id.AssetSnapshotId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BaseUrl
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.WalletAddress
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID

class AssetSnapshotControllerTest : TestBase() {

    companion object {
        private val PROJECT = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
    }

    @Test
    fun mustCorrectlyCreateAssetSnapshotAndReturnAResponse() {
        val service = mock<AssetSnapshotQueueService>()
        val name = "asset-snapshot-name"
        val assetAddress = ContractAddress("a")
        val request = CreateAssetSnapshotRequest(
            name = name,
            assetAddress = assetAddress.rawValue,
            payoutBlockNumber = BigInteger.TEN,
            ignoredHolderAddresses = setOf("f"),
        )
        val assetSnapshotUuid = AssetSnapshotId(UUID.randomUUID())

        suppose("asset snapshot will be submitted") {
            call(
                service.submitAssetSnapshot(
                    CreateAssetSnapshotParams(
                        name = name,
                        chainId = PROJECT.chainId,
                        projectId = PROJECT.id,
                        assetContractAddress = assetAddress,
                        payoutBlock = BlockNumber(request.payoutBlockNumber),
                        ignoredHolderAddresses = request.ignoredHolderAddresses
                            .mapTo(HashSet()) { WalletAddress(it) }
                    )
                )
            )
                .willReturn(assetSnapshotUuid)
        }

        val controller = AssetSnapshotController(service)

        verify("correct response is returned") {
            val response = controller.createAssetSnapshot(
                project = PROJECT,
                requestBody = request
            )

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(CreateAssetSnapshotResponse(assetSnapshotUuid)))
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSnapshotById() {
        val assetSnapshotUuid = AssetSnapshotId(UUID.randomUUID())
        val assetSnapshot = FullAssetSnapshot(
            id = assetSnapshotUuid,
            name = "asset-snapshot-name",
            chainId = ChainId(1L),
            projectId = PROJECT.id,
            assetContractAddress = ContractAddress("a"),
            blockNumber = BlockNumber(BigInteger.ONE),
            ignoredHolderAddresses = setOf(WalletAddress("b")),
            snapshotStatus = AssetSnapshotStatus.PENDING,
            snapshotFailureCause = null,
            data = null
        )

        val service = mock<AssetSnapshotQueueService>()

        suppose("some asset snapshot will be returned") {
            call(service.getAssetSnapshotById(assetSnapshotUuid))
                .willReturn(assetSnapshot)
        }

        val controller = AssetSnapshotController(service)

        verify("correct response is returned") {
            val response = controller.getAssetSnapshotById(assetSnapshotUuid)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(assetSnapshot.toAssetSnapshotResponse()))
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentAssetSnapshot() {
        val service = mock<AssetSnapshotQueueService>()

        suppose("null will be returned") {
            call(service.getAssetSnapshotById(anyValueClass(AssetSnapshotId(UUID.randomUUID()))))
                .willReturn(null)
        }

        val controller = AssetSnapshotController(service)

        verify("exception is thrown") {
            expectThrows<ResourceNotFoundException> {
                controller.getAssetSnapshotById(AssetSnapshotId(UUID.randomUUID()))
            }
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSnapshotsWithSomeStatusFilter() {
        val snapshots = listOf(
            createSuccessfulSnapshot(0),
            createSuccessfulSnapshot(1),
            createPendingSnapshot(2)
        )
        val service = mock<AssetSnapshotQueueService>()
        val status = listOf(
            AssetSnapshotStatus.PENDING,
            AssetSnapshotStatus.SUCCESS
        )

        suppose("some asset snapshots will be returned") {
            call(
                service.getAllAssetSnapshotsByProjectIdAndStatuses(
                    projectId = PROJECT.id,
                    statuses = status.toSet()
                )
            )
                .willReturn(snapshots)
        }

        val controller = AssetSnapshotController(service)

        verify("correct asset snapshots are returned") {
            val response = controller.getAssetSnapshots(
                projectId = PROJECT.id,
                status = status
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetSnapshotsResponse(snapshots.map { it.toAssetSnapshotResponse() })
                    )
                )
        }
    }

    private fun createPendingSnapshot(id: Int): FullAssetSnapshot {
        val uuid = AssetSnapshotId(UUID.randomUUID())
        return FullAssetSnapshot(
            id = uuid,
            name = "asset-snapshot-${uuid.value}",
            chainId = PROJECT.chainId,
            projectId = PROJECT.id,
            assetContractAddress = ContractAddress("aaa$id"),
            blockNumber = BlockNumber(BigInteger.valueOf(id * 100L)),
            ignoredHolderAddresses = emptySet(),
            snapshotStatus = AssetSnapshotStatus.PENDING,
            snapshotFailureCause = null,
            data = null
        )
    }

    private fun createSuccessfulSnapshot(id: Int): FullAssetSnapshot =
        createPendingSnapshot(id)
            .copy(
                snapshotStatus = AssetSnapshotStatus.SUCCESS,
                data = FullAssetSnapshotData(
                    totalAssetAmount = Balance(BigInteger.valueOf(id * 200L)),
                    merkleRootHash = MerkleHash("root-hash-$id"),
                    merkleTreeIpfsHash = IpfsHash("ipfs-hash-$id"),
                    merkleTreeDepth = id,
                    hashFn = HashFunction.KECCAK_256
                )
            )
}
