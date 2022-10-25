package dev3.blockchainapiservice.service

import dev3.blockchainapiservice.config.ApplicationProperties
import dev3.blockchainapiservice.exception.CannotDecompileContractBinaryException
import dev3.blockchainapiservice.exception.ContractDecompilationTemporarilyUnavailableException
import dev3.blockchainapiservice.model.json.DecompiledContractJson
import dev3.blockchainapiservice.util.ContractBinaryData
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException.BadRequest
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Service
class ExternalContractDecompilerService(
    private val externalContractDecompilerServiceRestTemplate: RestTemplate,
    private val applicationProperties: ApplicationProperties
) : ContractDecompilerService {

    companion object {
        data class Request(val bytecode: String)
    }

    override fun decompile(contractBinary: ContractBinaryData): DecompiledContractJson =
        try {
            externalContractDecompilerServiceRestTemplate.postForEntity(
                applicationProperties.contractManifestService.decompileContractPath,
                Request(contractBinary.value),
                DecompiledContractJson::class.java
            ).body ?: throw ContractDecompilationTemporarilyUnavailableException()
        } catch (e: BadRequest) {
            throw CannotDecompileContractBinaryException()
        } catch (e: RestClientException) {
            throw ContractDecompilationTemporarilyUnavailableException()
        }
}
