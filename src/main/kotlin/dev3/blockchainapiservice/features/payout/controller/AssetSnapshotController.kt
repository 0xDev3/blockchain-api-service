package dev3.blockchainapiservice.features.payout.controller

import dev3.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import dev3.blockchainapiservice.config.interceptors.annotation.ApiReadLimitedMapping
import dev3.blockchainapiservice.config.interceptors.annotation.ApiWriteLimitedMapping
import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.features.api.access.model.result.Project
import dev3.blockchainapiservice.features.payout.model.params.CreateAssetSnapshotParams
import dev3.blockchainapiservice.features.payout.model.request.CreateAssetSnapshotRequest
import dev3.blockchainapiservice.features.payout.model.response.AssetSnapshotResponse
import dev3.blockchainapiservice.features.payout.model.response.AssetSnapshotsResponse
import dev3.blockchainapiservice.features.payout.model.response.CreateAssetSnapshotResponse
import dev3.blockchainapiservice.features.payout.service.AssetSnapshotQueueService
import dev3.blockchainapiservice.features.payout.util.AssetSnapshotStatus
import dev3.blockchainapiservice.generated.jooq.id.AssetSnapshotId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.WalletAddress
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Validated
@RestController
class AssetSnapshotController(private val snapshotQueueService: AssetSnapshotQueueService) {

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/asset-snapshots")
    fun createAssetSnapshot(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateAssetSnapshotRequest
    ): ResponseEntity<CreateAssetSnapshotResponse> {
        val assetSnapshotId = snapshotQueueService.submitAssetSnapshot(
            CreateAssetSnapshotParams(
                name = requestBody.name,
                chainId = project.chainId,
                projectId = project.id,
                assetContractAddress = ContractAddress(requestBody.assetAddress),
                payoutBlock = BlockNumber(requestBody.payoutBlockNumber),
                ignoredHolderAddresses = requestBody.ignoredHolderAddresses.mapTo(HashSet()) { WalletAddress(it) }
            )
        )

        return ResponseEntity.ok(CreateAssetSnapshotResponse(assetSnapshotId))
    }

    @ApiReadLimitedMapping(IdType.ASSET_SNAPSHOT_ID, "/v1/asset-snapshots/{id}")
    fun getAssetSnapshotById(
        @PathVariable id: AssetSnapshotId
    ): ResponseEntity<AssetSnapshotResponse> {
        return snapshotQueueService.getAssetSnapshotById(id)
            ?.let { ResponseEntity.ok(it.toAssetSnapshotResponse()) }
            ?: throw ResourceNotFoundException("Asset snapshot not found")
    }

    @ApiReadLimitedMapping(IdType.PROJECT_ID, "/v1/asset-snapshots/by-project/{projectId}")
    fun getAssetSnapshots(
        @PathVariable("projectId") projectId: ProjectId,
        @RequestParam(required = false) status: List<AssetSnapshotStatus>?
    ): ResponseEntity<AssetSnapshotsResponse> {
        val assetSnapshots = snapshotQueueService.getAllAssetSnapshotsByProjectIdAndStatuses(
            projectId = projectId,
            statuses = status.orEmpty().toSet()
        )

        return ResponseEntity.ok(AssetSnapshotsResponse(assetSnapshots.map { it.toAssetSnapshotResponse() }))
    }
}
