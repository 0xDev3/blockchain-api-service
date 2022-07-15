package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import com.ampnet.blockchainapiservice.model.filters.AndList
import com.ampnet.blockchainapiservice.model.filters.ContractDeploymentRequestFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.params.CreateContractDeploymentRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachTransactionInfoRequest
import com.ampnet.blockchainapiservice.model.request.CreateContractDeploymentRequest
import com.ampnet.blockchainapiservice.model.response.ContractDeploymentRequestResponse
import com.ampnet.blockchainapiservice.model.response.ContractDeploymentRequestsResponse
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.service.ContractDeploymentRequestService
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ContractDeploymentRequestController(
    private val contractDeploymentRequestService: ContractDeploymentRequestService
) {

    @PostMapping("/v1/deploy")
    fun createContractDeploymentRequest(
        @ApiKeyBinding project: Project,
        @RequestBody requestBody: CreateContractDeploymentRequest
    ): ResponseEntity<ContractDeploymentRequestResponse> {
        val params = CreateContractDeploymentRequestParams(requestBody)
        val createdRequest = contractDeploymentRequestService.createContractDeploymentRequest(params, project)
        return ResponseEntity.ok(ContractDeploymentRequestResponse(createdRequest))
    }

    @GetMapping("/v1/deploy/{id}")
    fun getContractDeploymentRequest(
        @PathVariable("id") id: UUID
    ): ResponseEntity<ContractDeploymentRequestResponse> {
        val contractDeploymentRequest = contractDeploymentRequestService.getContractDeploymentRequest(id)
        return ResponseEntity.ok(ContractDeploymentRequestResponse(contractDeploymentRequest))
    }

    @GetMapping("/v1/deploy/by-project/{projectId}")
    fun getContractDeploymentRequestsByProjectIdAndFilters(
        @PathVariable("projectId") projectId: UUID,
        @RequestParam("contractIds", required = false) contractIds: List<String>?,
        @RequestParam("contractTags", required = false) contractTags: List<String>?,
        @RequestParam("contractImplements", required = false) contractImplements: List<String>?,
        @RequestParam("deployedOnly", required = false, defaultValue = "false") deployedOnly: Boolean,
    ): ResponseEntity<ContractDeploymentRequestsResponse> {
        val contractDeploymentRequests = contractDeploymentRequestService
            .getContractDeploymentRequestsByProjectIdAndFilters(
                projectId = projectId,
                filters = ContractDeploymentRequestFilters(
                    contractIds = OrList(contractIds.orEmpty().map { ContractId(it) }),
                    contractTags = contractTags.toOrListWithNestedAndLists { ContractTag(it) },
                    contractImplements = contractImplements.toOrListWithNestedAndLists { ContractTrait(it) },
                    deployedOnly = deployedOnly
                )
            )
        return ResponseEntity.ok(
            ContractDeploymentRequestsResponse(contractDeploymentRequests.map { ContractDeploymentRequestResponse(it) })
        )
    }

    @PutMapping("/v1/deploy/{id}")
    fun attachTransactionInfo(
        @PathVariable("id") id: UUID,
        @RequestBody requestBody: AttachTransactionInfoRequest
    ) {
        contractDeploymentRequestService.attachTxInfo(
            id = id,
            txHash = TransactionHash(requestBody.txHash),
            deployer = WalletAddress(requestBody.callerAddress)
        )
    }

    private fun <T> List<String>?.toOrListWithNestedAndLists(wrap: (String) -> T): OrList<AndList<T>> =
        OrList(this.orEmpty().map { AndList(it.split(" AND ").map { wrap(it) }) })
}
