package dev3.blockchainapiservice.controller

import dev3.blockchainapiservice.exception.ResourceNotFoundException
import dev3.blockchainapiservice.model.response.ContractInterfaceManifestResponse
import dev3.blockchainapiservice.model.response.ContractInterfaceManifestsResponse
import dev3.blockchainapiservice.model.response.InfoMarkdownsResponse
import dev3.blockchainapiservice.repository.ContractInterfacesRepository
import dev3.blockchainapiservice.util.InterfaceId
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class ContractInterfacesController(
    private val contractInterfacesRepository: ContractInterfacesRepository
) {

    @GetMapping("/v1/contract-interfaces")
    fun getContractInterfaces(): ResponseEntity<ContractInterfaceManifestsResponse> {
        val contractInterfaces = contractInterfacesRepository.getAll()
        return ResponseEntity.ok(
            ContractInterfaceManifestsResponse(
                contractInterfaces.map { ContractInterfaceManifestResponse(it) }
            )
        )
    }

    @GetMapping("/v1/contract-interfaces/info.md")
    fun getContractInterfaceInfoMarkdownFiles(): ResponseEntity<InfoMarkdownsResponse> {
        val interfaceInfoMarkdowns = contractInterfacesRepository.getAllInfoMarkdownFiles()
        return ResponseEntity.ok(InfoMarkdownsResponse(interfaceInfoMarkdowns))
    }

    @GetMapping("/v1/contract-interfaces/{id}")
    fun getContractInterface(
        @PathVariable("id") id: String
    ): ResponseEntity<ContractInterfaceManifestResponse> {
        val interfaceId = InterfaceId(id)
        val contractInterface = contractInterfacesRepository.getById(interfaceId)
            ?: throw ResourceNotFoundException("Contract interface not found for interface ID: $id")
        return ResponseEntity.ok(ContractInterfaceManifestResponse(interfaceId, contractInterface))
    }

    @GetMapping(
        path = ["/v1/contract-interfaces/{id}/info.md"],
        produces = [MediaType.TEXT_MARKDOWN_VALUE]
    )
    fun getContractInterfaceInfoMarkdown(
        @PathVariable("id") id: String
    ): ResponseEntity<String> {
        val interfaceId = InterfaceId(id)
        val infoMarkdown = contractInterfacesRepository.getInfoMarkdownById(interfaceId)
            ?: throw ResourceNotFoundException("Contract interface info.md not found for interface ID: $id")
        return ResponseEntity.ok(infoMarkdown)
    }
}