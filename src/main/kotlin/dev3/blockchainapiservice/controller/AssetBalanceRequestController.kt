package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import dev3.blockchainapiservice.config.interceptors.annotation.ApiReadLimitedMapping
import dev3.blockchainapiservice.config.interceptors.annotation.ApiWriteLimitedMapping
import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import dev3.blockchainapiservice.generated.jooq.id.AssetBalanceRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.params.CreateAssetBalanceRequestParams
import dev3.blockchainapiservice.model.request.AttachSignedMessageRequest
import dev3.blockchainapiservice.model.request.CreateAssetBalanceRequest
import dev3.blockchainapiservice.model.response.AssetBalanceRequestResponse
import dev3.blockchainapiservice.model.response.AssetBalanceRequestsResponse
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.service.AssetBalanceRequestService
import dev3.blockchainapiservice.util.SignedMessage
import dev3.blockchainapiservice.util.WalletAddress
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Validated
@RestController
class AssetBalanceRequestController(private val assetBalanceRequestService: AssetBalanceRequestService) {

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/balance")
    fun createAssetBalanceRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateAssetBalanceRequest
    ): ResponseEntity<AssetBalanceRequestResponse> {
        val params = CreateAssetBalanceRequestParams(requestBody)
        val createdRequest = assetBalanceRequestService.createAssetBalanceRequest(params, project)
        return ResponseEntity.ok(AssetBalanceRequestResponse(createdRequest))
    }

    @ApiReadLimitedMapping(IdType.ASSET_BALANCE_REQUEST_ID, "/v1/balance/{id}")
    fun getAssetBalanceRequest(
        @PathVariable("id") id: AssetBalanceRequestId
    ): ResponseEntity<AssetBalanceRequestResponse> {
        val balanceRequest = assetBalanceRequestService.getAssetBalanceRequest(id)
        return ResponseEntity.ok(AssetBalanceRequestResponse(balanceRequest))
    }

    @ApiReadLimitedMapping(IdType.PROJECT_ID, "/v1/balance/by-project/{projectId}")
    fun getAssetBalanceRequestsByProjectId(
        @PathVariable("projectId") projectId: ProjectId
    ): ResponseEntity<AssetBalanceRequestsResponse> {
        val balanceRequests = assetBalanceRequestService.getAssetBalanceRequestsByProjectId(projectId)
        return ResponseEntity.ok(AssetBalanceRequestsResponse(balanceRequests.map { AssetBalanceRequestResponse(it) }))
    }

    @ApiWriteLimitedMapping(IdType.ASSET_BALANCE_REQUEST_ID, RequestMethod.PUT, "/v1/balance/{id}")
    fun attachSignedMessage(
        @PathVariable("id") id: AssetBalanceRequestId,
        @Valid @RequestBody requestBody: AttachSignedMessageRequest
    ) {
        assetBalanceRequestService.attachWalletAddressAndSignedMessage(
            id = id,
            walletAddress = WalletAddress(requestBody.walletAddress),
            signedMessage = SignedMessage(requestBody.signedMessage)
        )
    }
}
