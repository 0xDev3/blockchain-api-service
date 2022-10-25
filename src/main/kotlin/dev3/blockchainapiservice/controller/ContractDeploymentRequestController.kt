package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import dev3.blockchainapiservice.config.validation.MaxStringSize
import dev3.blockchainapiservice.model.filters.ContractDeploymentRequestFilters
import dev3.blockchainapiservice.model.filters.OrList
import dev3.blockchainapiservice.model.filters.parseOrListWithNestedAndLists
import dev3.blockchainapiservice.model.params.CreateContractDeploymentRequestParams
import dev3.blockchainapiservice.model.request.AttachTransactionInfoRequest
import dev3.blockchainapiservice.model.request.CreateContractDeploymentRequest
import dev3.blockchainapiservice.model.response.ContractDeploymentRequestResponse
import dev3.blockchainapiservice.model.response.ContractDeploymentRequestsResponse
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.service.ContractDeploymentRequestService
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.ContractTag
import dev3.blockchainapiservice.util.InterfaceId
import dev3.blockchainapiservice.util.TransactionHash
import dev3.blockchainapiservice.util.WalletAddress
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@Validated
@RestController
class ContractDeploymentRequestController(
    private val contractDeploymentRequestService: ContractDeploymentRequestService
) {

    @PostMapping("/v1/deploy")
    fun createContractDeploymentRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateContractDeploymentRequest
    ): ResponseEntity<ContractDeploymentRequestResponse> {
        val params = CreateContractDeploymentRequestParams(requestBody)
        val createdRequest = contractDeploymentRequestService.createContractDeploymentRequest(params, project)
        return ResponseEntity.ok(ContractDeploymentRequestResponse(createdRequest))
    }

    @DeleteMapping("/v1/deploy/{id}")
    fun markContractDeploymentRequestAsDeleted(
        @ApiKeyBinding project: Project,
        @PathVariable("id") id: UUID
    ) {
        contractDeploymentRequestService.markContractDeploymentRequestAsDeleted(id, project.id)
    }

    @GetMapping("/v1/deploy/{id}")
    fun getContractDeploymentRequest(
        @PathVariable("id") id: UUID
    ): ResponseEntity<ContractDeploymentRequestResponse> {
        val contractDeploymentRequest = contractDeploymentRequestService.getContractDeploymentRequest(id)
        return ResponseEntity.ok(ContractDeploymentRequestResponse(contractDeploymentRequest))
    }

    @GetMapping("/v1/deploy/by-project/{projectId}/by-alias/{alias}")
    fun getContractDeploymentRequestByProjectIdAndAlias(
        @PathVariable("projectId") projectId: UUID,
        @PathVariable("alias") alias: String
    ): ResponseEntity<ContractDeploymentRequestResponse> {
        val contractDeploymentRequest = contractDeploymentRequestService
            .getContractDeploymentRequestByProjectIdAndAlias(
                projectId = projectId,
                alias = alias
            )
        return ResponseEntity.ok(ContractDeploymentRequestResponse(contractDeploymentRequest))
    }

    @GetMapping("/v1/deploy/by-project/{projectId}")
    fun getContractDeploymentRequestsByProjectIdAndFilters(
        @PathVariable("projectId") projectId: UUID,
        @Valid @RequestParam("contractIds", required = false) contractIds: List<@MaxStringSize String>?,
        @Valid @RequestParam("contractTags", required = false) contractTags: List<@MaxStringSize String>?,
        @Valid @RequestParam("contractImplements", required = false) contractImplements: List<@MaxStringSize String>?,
        @RequestParam("deployedOnly", required = false, defaultValue = "false") deployedOnly: Boolean,
    ): ResponseEntity<ContractDeploymentRequestsResponse> {
        val contractDeploymentRequests = contractDeploymentRequestService
            .getContractDeploymentRequestsByProjectIdAndFilters(
                projectId = projectId,
                filters = ContractDeploymentRequestFilters(
                    contractIds = OrList(contractIds.orEmpty().map { ContractId(it) }),
                    contractTags = contractTags.parseOrListWithNestedAndLists { ContractTag(it) },
                    contractImplements = contractImplements.parseOrListWithNestedAndLists { InterfaceId(it) },
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
        @Valid @RequestBody requestBody: AttachTransactionInfoRequest
    ) {
        contractDeploymentRequestService.attachTxInfo(
            id = id,
            txHash = TransactionHash(requestBody.txHash),
            deployer = WalletAddress(requestBody.callerAddress)
        )
    }
}
