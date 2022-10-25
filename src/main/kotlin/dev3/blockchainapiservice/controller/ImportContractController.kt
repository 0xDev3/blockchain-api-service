package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import dev3.blockchainapiservice.model.params.ImportContractParams
import dev3.blockchainapiservice.model.request.ImportContractRequest
import dev3.blockchainapiservice.model.request.ImportedContractInterfacesRequest
import dev3.blockchainapiservice.model.response.ContractDeploymentRequestResponse
import dev3.blockchainapiservice.model.response.ContractInterfaceManifestResponse
import dev3.blockchainapiservice.model.response.ContractInterfaceManifestsResponse
import dev3.blockchainapiservice.model.result.Project
import dev3.blockchainapiservice.service.ContractDeploymentRequestService
import dev3.blockchainapiservice.service.ContractImportService
import dev3.blockchainapiservice.service.ContractInterfacesService
import dev3.blockchainapiservice.util.InterfaceId
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@Validated
@RestController
class ImportContractController(
    private val contractImportService: ContractImportService,
    private val contractDeploymentRequestService: ContractDeploymentRequestService,
    private val contractInterfacesService: ContractInterfacesService
) {

    @PostMapping("/v1/import-smart-contract")
    fun importSmartContract(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: ImportContractRequest
    ): ResponseEntity<ContractDeploymentRequestResponse> {
        val params = ImportContractParams(requestBody)
        val importedContractId = contractImportService.importContract(params, project)
        val importedContract = contractDeploymentRequestService.getContractDeploymentRequest(importedContractId)
        return ResponseEntity.ok(ContractDeploymentRequestResponse(importedContract))
    }

    @GetMapping("/v1/import-smart-contract/{id}/suggested-interfaces")
    fun getSuggestedInterfacesForImportedSmartContract(
        @PathVariable("id") id: UUID
    ): ResponseEntity<ContractInterfaceManifestsResponse> {
        val manifests = contractInterfacesService.getSuggestedInterfacesForImportedSmartContract(id)
        return ResponseEntity.ok(
            ContractInterfaceManifestsResponse(
                manifests.map { ContractInterfaceManifestResponse(it) }
            )
        )
    }

    @PatchMapping("/v1/import-smart-contract/{id}/add-interfaces")
    fun addInterfacesToImportedSmartContract(
        @ApiKeyBinding project: Project,
        @PathVariable("id") id: UUID,
        @Valid @RequestBody requestBody: ImportedContractInterfacesRequest
    ): ResponseEntity<ContractDeploymentRequestResponse> {
        contractInterfacesService.addInterfacesToImportedContract(
            importedContractId = id,
            projectId = project.id,
            interfaces = requestBody.interfaces.map { InterfaceId(it) }
        )

        val importedContract = contractDeploymentRequestService.getContractDeploymentRequest(id)

        return ResponseEntity.ok(ContractDeploymentRequestResponse(importedContract))
    }

    @PatchMapping("/v1/import-smart-contract/{id}/remove-interfaces")
    fun removeInterfacesFromImportedSmartContract(
        @ApiKeyBinding project: Project,
        @PathVariable("id") id: UUID,
        @Valid @RequestBody requestBody: ImportedContractInterfacesRequest
    ): ResponseEntity<ContractDeploymentRequestResponse> {
        contractInterfacesService.removeInterfacesFromImportedContract(
            importedContractId = id,
            projectId = project.id,
            interfaces = requestBody.interfaces.map { InterfaceId(it) }
        )

        val importedContract = contractDeploymentRequestService.getContractDeploymentRequest(id)

        return ResponseEntity.ok(ContractDeploymentRequestResponse(importedContract))
    }

    @PatchMapping("/v1/import-smart-contract/{id}/set-interfaces")
    fun setInterfacesForImportedSmartContract(
        @ApiKeyBinding project: Project,
        @PathVariable("id") id: UUID,
        @Valid @RequestBody requestBody: ImportedContractInterfacesRequest
    ): ResponseEntity<ContractDeploymentRequestResponse> {
        contractInterfacesService.setImportedContractInterfaces(
            importedContractId = id,
            projectId = project.id,
            interfaces = requestBody.interfaces.map { InterfaceId(it) }
        )

        val importedContract = contractDeploymentRequestService.getContractDeploymentRequest(id)

        return ResponseEntity.ok(ContractDeploymentRequestResponse(importedContract))
    }
}
