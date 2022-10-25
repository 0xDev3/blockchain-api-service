package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.config.validation.MaxStringSize
import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.filters.ContractDecoratorFilters
import dev3.blockchainapiservice.model.filters.parseOrListWithNestedAndLists
import dev3.blockchainapiservice.model.json.ArtifactJson
import dev3.blockchainapiservice.model.json.ManifestJson
import dev3.blockchainapiservice.model.response.ArtifactJsonsResponse
import dev3.blockchainapiservice.model.response.ContractDecoratorResponse
import dev3.blockchainapiservice.model.response.ContractDecoratorsResponse
import dev3.blockchainapiservice.model.response.InfoMarkdownsResponse
import dev3.blockchainapiservice.model.response.ManifestJsonsResponse
import dev3.blockchainapiservice.repository.ContractDecoratorRepository
import dev3.blockchainapiservice.repository.ImportedContractDecoratorRepository
import dev3.blockchainapiservice.util.ContractId
import dev3.blockchainapiservice.util.ContractTag
import dev3.blockchainapiservice.util.InterfaceId
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@Validated
@RestController
class ContractDecoratorController(
    private val contractDecoratorRepository: ContractDecoratorRepository,
    private val importedContractDecoratorRepository: ImportedContractDecoratorRepository
) {

    @GetMapping("/v1/deployable-contracts")
    fun getContractDecorators(
        @Valid @RequestParam("tags", required = false) contractTags: List<@MaxStringSize String>?,
        @Valid @RequestParam("implements", required = false) contractImplements: List<@MaxStringSize String>?,
        @RequestParam("projectId", required = false) projectId: UUID?
    ): ResponseEntity<ContractDecoratorsResponse> {
        val filters = ContractDecoratorFilters(
            contractTags = contractTags.parseOrListWithNestedAndLists { ContractTag(it) },
            contractImplements = contractImplements.parseOrListWithNestedAndLists { InterfaceId(it) }
        )
        val contractDecorators = contractDecoratorRepository.getAll(filters) +
            projectId.getIfPresent { importedContractDecoratorRepository.getAll(it, filters) }
        return ResponseEntity.ok(ContractDecoratorsResponse(contractDecorators.map { ContractDecoratorResponse(it) }))
    }

    @GetMapping("/v1/deployable-contracts/manifest.json")
    fun getContractManifestJsonFiles(
        @Valid @RequestParam("tags", required = false) contractTags: List<@MaxStringSize String>?,
        @Valid @RequestParam("implements", required = false) contractImplements: List<@MaxStringSize String>?,
        @RequestParam("projectId", required = false) projectId: UUID?
    ): ResponseEntity<ManifestJsonsResponse> {
        val filters = ContractDecoratorFilters(
            contractTags = contractTags.parseOrListWithNestedAndLists { ContractTag(it) },
            contractImplements = contractImplements.parseOrListWithNestedAndLists { InterfaceId(it) }
        )
        val contractManifests = contractDecoratorRepository.getAllManifestJsonFiles(filters) +
            projectId.getIfPresent { importedContractDecoratorRepository.getAllManifestJsonFiles(it, filters) }
        return ResponseEntity.ok(ManifestJsonsResponse(contractManifests))
    }

    @GetMapping("/v1/deployable-contracts/artifact.json")
    fun getContractArtifactJsonFiles(
        @Valid @RequestParam("tags", required = false) contractTags: List<@MaxStringSize String>?,
        @Valid @RequestParam("implements", required = false) contractImplements: List<@MaxStringSize String>?,
        @RequestParam("projectId", required = false) projectId: UUID?
    ): ResponseEntity<ArtifactJsonsResponse> {
        val filters = ContractDecoratorFilters(
            contractTags = contractTags.parseOrListWithNestedAndLists { ContractTag(it) },
            contractImplements = contractImplements.parseOrListWithNestedAndLists { InterfaceId(it) }
        )
        val contractArtifacts = contractDecoratorRepository.getAllArtifactJsonFiles(filters) +
            projectId.getIfPresent { importedContractDecoratorRepository.getAllArtifactJsonFiles(it, filters) }
        return ResponseEntity.ok(ArtifactJsonsResponse(contractArtifacts))
    }

    @GetMapping("/v1/deployable-contracts/info.md")
    fun getContractInfoMarkdownFiles(
        @Valid @RequestParam("tags", required = false) contractTags: List<@MaxStringSize String>?,
        @Valid @RequestParam("implements", required = false) contractImplements: List<@MaxStringSize String>?,
        @RequestParam("projectId", required = false) projectId: UUID?
    ): ResponseEntity<InfoMarkdownsResponse> {
        val filters = ContractDecoratorFilters(
            contractTags = contractTags.parseOrListWithNestedAndLists { ContractTag(it) },
            contractImplements = contractImplements.parseOrListWithNestedAndLists { InterfaceId(it) }
        )
        val contractInfoMarkdowns = contractDecoratorRepository.getAllInfoMarkdownFiles(filters) +
            projectId.getIfPresent { importedContractDecoratorRepository.getAllInfoMarkdownFiles(it, filters) }
        return ResponseEntity.ok(InfoMarkdownsResponse(contractInfoMarkdowns))
    }

    @GetMapping("/v1/deployable-contracts/{id}")
    fun getContractDecorator(
        @PathVariable("id") id: String,
        @RequestParam("projectId", required = false) projectId: UUID?
    ): ResponseEntity<ContractDecoratorResponse> {
        val contractId = ContractId(id)
        val contractDecorator = contractDecoratorRepository.getById(contractId)
            ?: projectId?.let { importedContractDecoratorRepository.getByContractIdAndProjectId(contractId, it) }
            ?: throw ResourceNotFoundException("Contract decorator not found for contract ID: $id")
        return ResponseEntity.ok(ContractDecoratorResponse(contractDecorator))
    }

    @GetMapping("/v1/deployable-contracts/{id}/manifest.json")
    fun getContractManifestJson(
        @PathVariable("id") id: String,
        @RequestParam("projectId", required = false) projectId: UUID?
    ): ResponseEntity<ManifestJson> {
        val contractId = ContractId(id)
        val manifestJson = contractDecoratorRepository.getManifestJsonById(contractId)
            ?: projectId
                ?.let { importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(contractId, it) }
            ?: throw ResourceNotFoundException("Contract manifest.json not found for contract ID: $id")
        return ResponseEntity.ok(manifestJson)
    }

    @GetMapping("/v1/deployable-contracts/{id}/artifact.json")
    fun getContractArtifactJson(
        @PathVariable("id") id: String,
        @RequestParam("projectId", required = false) projectId: UUID?
    ): ResponseEntity<ArtifactJson> {
        val contractId = ContractId(id)
        val artifactJson = contractDecoratorRepository.getArtifactJsonById(contractId)
            ?: projectId
                ?.let { importedContractDecoratorRepository.getArtifactJsonByContractIdAndProjectId(contractId, it) }
            ?: throw ResourceNotFoundException("Contract artifact.json not found for contract ID: $id")
        return ResponseEntity.ok(artifactJson)
    }

    @GetMapping(
        path = ["/v1/deployable-contracts/{id}/info.md"],
        produces = [MediaType.TEXT_MARKDOWN_VALUE]
    )
    fun getContractInfoMarkdown(
        @PathVariable("id") id: String,
        @RequestParam("projectId", required = false) projectId: UUID?
    ): ResponseEntity<String> {
        val contractId = ContractId(id)
        val infoMarkdown = contractDecoratorRepository.getInfoMarkdownById(contractId)
            ?: projectId
                ?.let { importedContractDecoratorRepository.getInfoMarkdownByContractIdAndProjectId(contractId, it) }
            ?: throw ResourceNotFoundException("Contract info.md not found for contract ID: $id")
        return ResponseEntity.ok(infoMarkdown)
    }

    private fun <T> UUID?.getIfPresent(fn: (UUID) -> List<T>): List<T> = if (this != null) fn(this) else emptyList()
}
