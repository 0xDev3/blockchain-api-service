package dev3.blockchainapiservice.features.payout.service

import com.fasterxml.jackson.databind.JsonNode
import dev3.blockchainapiservice.exception.IpfsUploadFailedException
import dev3.blockchainapiservice.features.payout.model.json.PinataResponse
import dev3.blockchainapiservice.features.payout.util.IpfsHash
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Service
class PinataIpfsService(private val pinataRestTemplate: RestTemplate) : IpfsService {

    companion object : KLogging()

    override fun pinJsonToIpfs(json: JsonNode): IpfsHash {
        try {
            val response = pinataRestTemplate.postForEntity("/pinning/pinJSONToIPFS", json, PinataResponse::class.java)

            if (response.statusCode.is2xxSuccessful) {
                return response.body?.ipfsHash?.let { IpfsHash(it) } ?: run {
                    logger.warn { "IPFS hash is missing on upload response" }
                    throw IpfsUploadFailedException()
                }
            }

            logger.warn { "IPFS upload failed" }
            throw IpfsUploadFailedException()
        } catch (ex: RestClientException) {
            logger.warn(ex) { "IPFS client call exception" }
            throw IpfsUploadFailedException()
        }
    }
}
