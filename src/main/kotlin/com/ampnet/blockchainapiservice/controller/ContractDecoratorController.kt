package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.config.validation.MaxStringSize
import com.ampnet.blockchainapiservice.exception.ResourceNotFoundException
import com.ampnet.blockchainapiservice.model.filters.ContractDecoratorFilters
import com.ampnet.blockchainapiservice.model.filters.parseOrListWithNestedAndLists
import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.ampnet.blockchainapiservice.model.response.ArtifactJsonsResponse
import com.ampnet.blockchainapiservice.model.response.ContractDecoratorResponse
import com.ampnet.blockchainapiservice.model.response.ContractDecoratorsResponse
import com.ampnet.blockchainapiservice.model.response.ManifestJsonsResponse
import com.ampnet.blockchainapiservice.repository.ContractDecoratorRepository
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Validated
@RestController
class ContractDecoratorController(private val contractDecoratorRepository: ContractDecoratorRepository) {

    @GetMapping("/v1/deployable-contracts")
    fun getContractDecorators(
        @Valid @RequestParam("tags", required = false) contractTags: List<@MaxStringSize String>?,
        @Valid @RequestParam("implements", required = false) contractImplements: List<@MaxStringSize String>?
    ): ResponseEntity<ContractDecoratorsResponse> {
        val contractDecorators = contractDecoratorRepository.getAll(
            ContractDecoratorFilters(
                contractTags = contractTags.parseOrListWithNestedAndLists { ContractTag(it) },
                contractImplements = contractImplements.parseOrListWithNestedAndLists { ContractTrait(it) }
            )
        )
        return ResponseEntity.ok(ContractDecoratorsResponse(contractDecorators.map { ContractDecoratorResponse(it) }))
    }

    @GetMapping("/v1/deployable-contracts/manifest.json")
    fun getContractManifestJsonFiles(
        @Valid @RequestParam("tags", required = false) contractTags: List<@MaxStringSize String>?,
        @Valid @RequestParam("implements", required = false) contractImplements: List<@MaxStringSize String>?
    ): ResponseEntity<ManifestJsonsResponse> {
        val contractManifests = contractDecoratorRepository.getAllManifestJsonFiles(
            ContractDecoratorFilters(
                contractTags = contractTags.parseOrListWithNestedAndLists { ContractTag(it) },
                contractImplements = contractImplements.parseOrListWithNestedAndLists { ContractTrait(it) }
            )
        )
        return ResponseEntity.ok(ManifestJsonsResponse(contractManifests))
    }

    @GetMapping("/v1/deployable-contracts/artifact.json")
    fun getContractArtifactJsonFiles(
        @Valid @RequestParam("tags", required = false) contractTags: List<@MaxStringSize String>?,
        @Valid @RequestParam("implements", required = false) contractImplements: List<@MaxStringSize String>?
    ): ResponseEntity<ArtifactJsonsResponse> {
        val contractArtifacts = contractDecoratorRepository.getAllArtifactJsonFiles(
            ContractDecoratorFilters(
                contractTags = contractTags.parseOrListWithNestedAndLists { ContractTag(it) },
                contractImplements = contractImplements.parseOrListWithNestedAndLists { ContractTrait(it) }
            )
        )
        return ResponseEntity.ok(ArtifactJsonsResponse(contractArtifacts))
    }

    @GetMapping("/v1/deployable-contracts/{id}")
    fun getContractDecorator(
        @PathVariable("id") id: String
    ): ResponseEntity<ContractDecoratorResponse> {
        val contractDecorator = contractDecoratorRepository.getById(ContractId(id)) ?: throw ResourceNotFoundException(
            "Contract decorator not found for contract ID: $id"
        )
        return ResponseEntity.ok(ContractDecoratorResponse(contractDecorator))
    }

    @GetMapping("/v1/deployable-contracts/{id}/manifest.json")
    fun getContractManifestJson(
        @PathVariable("id") id: String
    ): ResponseEntity<ManifestJson> {
        val manifestJson = contractDecoratorRepository.getManifestJsonById(ContractId(id))
            ?: throw ResourceNotFoundException("Contract manifest.json not found for contract ID: $id")
        return ResponseEntity.ok(manifestJson)
    }

    @GetMapping("/v1/deployable-contracts/{id}/artifact.json")
    fun getContractArtifactJson(
        @PathVariable("id") id: String
    ): ResponseEntity<ArtifactJson> {
        val artifactJson = contractDecoratorRepository.getArtifactJsonById(ContractId(id))
            ?: throw ResourceNotFoundException("Contract artifact.json not found for contract ID: $id")
        return ResponseEntity.ok(artifactJson)
    }
}
