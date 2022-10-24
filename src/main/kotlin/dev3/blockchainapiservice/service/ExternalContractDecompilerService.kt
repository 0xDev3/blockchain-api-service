package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.exception.CannotDecompileContractBinaryException
import com.ampnet.blockchainapiservice.exception.ContractDecompilationTemporarilyUnavailableException
import com.ampnet.blockchainapiservice.model.json.DecompiledContractJson
import com.ampnet.blockchainapiservice.util.ContractBinaryData
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
