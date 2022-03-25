package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.config.binding.annotation.ChainBinding
import com.ampnet.blockchainapiservice.model.response.FetchErc20TokenBalanceResponse
import com.ampnet.blockchainapiservice.service.BlockchainInfoService
import com.ampnet.blockchainapiservice.util.BlockName
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ContractAddress
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class BlockchainInfoController(private val blockchainInfoService: BlockchainInfoService) {

    companion object : KLogging()

    @GetMapping("/info/{chainId}/{messageId}/erc20-balance/{contractAddress}")
    fun fetchErc20TokenBalance(
        @ChainBinding chainSpec: ChainSpec,
        @PathVariable messageId: UUID,
        @PathVariable contractAddress: ContractAddress,
        @RequestParam(required = false) blockNumber: BlockNumber?
    ): ResponseEntity<FetchErc20TokenBalanceResponse> {
        val block = blockNumber ?: BlockName.LATEST

        logger.debug {
            "Fetching ERC20 balance, chainSpec: $chainSpec, messageId: $messageId," +
                " contractAddress: $contractAddress, block: $block"
        }

        val accountBalance = blockchainInfoService.fetchErc20AccountBalanceFromSignedMessage(
            messageId = messageId,
            chainId = chainSpec.chainId,
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
