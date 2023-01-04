package dev3.blockchainapiservice.features.contract.importing.service

import dev3.blockchainapiservice.config.ContractManifestServiceProperties
import dev3.blockchainapiservice.exception.CannotDecompileContractBinaryException
import dev3.blockchainapiservice.exception.ContractDecompilationTemporarilyUnavailableException
import dev3.blockchainapiservice.features.contract.importing.model.json.DecompiledContractJson
import dev3.blockchainapiservice.util.ContractBinaryData
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException.BadRequest
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Service
class ExternalContractDecompilerService(
    private val externalContractDecompilerServiceRestTemplate: RestTemplate,
    private val contractManifestServiceProperties: ContractManifestServiceProperties
) : ContractDecompilerService {

    companion object {
        private data class Request(val bytecode: String)
    }

    override fun decompile(contractBinary: ContractBinaryData): DecompiledContractJson =
        try {
            externalContractDecompilerServiceRestTemplate.postForEntity(
                contractManifestServiceProperties.decompileContractPath,
                Request(contractBinary.value),
                DecompiledContractJson::class.java
            ).body ?: throw ContractDecompilationTemporarilyUnavailableException()
        } catch (e: BadRequest) {
            throw CannotDecompileContractBinaryException()
        } catch (e: RestClientException) {
            throw ContractDecompilationTemporarilyUnavailableException()
        }
}
