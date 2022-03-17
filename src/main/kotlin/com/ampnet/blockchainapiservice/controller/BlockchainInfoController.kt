package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.model.response.FetchErc20TokenBalanceResponse
import com.ampnet.blockchainapiservice.service.BlockchainInfoService
import com.ampnet.blockchainapiservice.util.BlockName
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigInteger
import java.util.UUID

@RestController
class BlockchainInfoController(private val blockchainInfoService: BlockchainInfoService) {

    companion object : KLogging()

    @GetMapping("/info/{chainId}/{messageId}/erc20-balance/{contractAddress}")
    fun fetchErc20TokenBalance(
        @PathVariable("chainId") rawChainId: Long,
        @PathVariable("messageId") messageId: UUID,
        @PathVariable("contractAddress") rawContractAddress: String,
        @RequestParam(required = false) blockNumber: BigInteger?
    ): ResponseEntity<FetchErc20TokenBalanceResponse> {
        val chainId = ChainId(rawChainId)
        val contractAddress = ContractAddress(rawContractAddress)
        val block = blockNumber?.let { BlockNumber(it) } ?: BlockName.LATEST

        logger.debug {
            "Fetching ERC20 balance, chainId: $chainId, messageId: $messageId," +
                " contractAddress: $contractAddress, block: $block"
        }

        val accountBalance = blockchainInfoService.fetchErc20AccountBalanceFromSignedMessage(
            messageId = messageId,
            chainId = chainId,
            contractAddress = contractAddress,
            block = block
        )

        return ResponseEntity.ok(
            FetchErc20TokenBalanceResponse(
                walletAddress = accountBalance.address.rawValue,
                tokenBalance = accountBalance.balance.rawValue,
                tokenAddress = contractAddress.rawValue
            )
        )
    }
}
