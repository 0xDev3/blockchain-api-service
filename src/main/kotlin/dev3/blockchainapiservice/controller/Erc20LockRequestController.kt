package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import dev3.blockchainapiservice.config.interceptors.annotation.ApiReadLimitedMapping
import dev3.blockchainapiservice.config.interceptors.annotation.ApiWriteLimitedMapping
import dev3.blockchainapiservice.config.interceptors.annotation.IdType
import dev3.blockchainapiservice.generated.jooq.id.Erc20LockRequestId
import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.model.params.CreateErc20LockRequestParams
import dev3.blockchainapiservice.model.request.AttachTransactionInfoRequest
import dev3.blockchainapiservice.model.request.CreateErc20LockRequest
import dev3.blockchainapiservice.model.response.Erc20LockRequestResponse
import dev3.blockchainapiservice.model.response.Erc20LockRequestsResponse
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.service.Erc20LockRequestService
import dev3.blockchainapiservice.util.TransactionHash
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
class Erc20LockRequestController(private val erc20LockRequestService: Erc20LockRequestService) {

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/lock")
    fun createErc20LockRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateErc20LockRequest
    ): ResponseEntity<Erc20LockRequestResponse> {
        val params = CreateErc20LockRequestParams(requestBody)
        val createdRequest = erc20LockRequestService.createErc20LockRequest(params, project)
        return ResponseEntity.ok(Erc20LockRequestResponse(createdRequest))
    }

    @ApiReadLimitedMapping(IdType.ERC20_LOCK_REQUEST_ID, "/v1/lock/{id}")
    fun getErc20LockRequest(
        @PathVariable("id") id: Erc20LockRequestId
    ): ResponseEntity<Erc20LockRequestResponse> {
        val lockRequest = erc20LockRequestService.getErc20LockRequest(id)
        return ResponseEntity.ok(Erc20LockRequestResponse(lockRequest))
    }

    @ApiReadLimitedMapping(IdType.PROJECT_ID, "/v1/lock/by-project/{projectId}")
    fun getErc20LockRequestsByProjectId(
        @PathVariable("projectId") projectId: ProjectId
    ): ResponseEntity<Erc20LockRequestsResponse> {
        val lockRequests = erc20LockRequestService.getErc20LockRequestsByProjectId(projectId)
        return ResponseEntity.ok(Erc20LockRequestsResponse(lockRequests.map { Erc20LockRequestResponse(it) }))
    }

    @ApiWriteLimitedMapping(IdType.ERC20_LOCK_REQUEST_ID, RequestMethod.PUT, "/v1/lock/{id}")
    fun attachTransactionInfo(
        @PathVariable("id") id: Erc20LockRequestId,
        @Valid @RequestBody requestBody: AttachTransactionInfoRequest
    ) {
        erc20LockRequestService.attachTxInfo(
            id = id,
            txHash = TransactionHash(requestBody.txHash),
            caller = WalletAddress(requestBody.callerAddress)
        )
    }
}
