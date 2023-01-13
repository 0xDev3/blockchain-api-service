package dev3.blockchainapiservice.features.payout.controller

import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.config.binding.annotation.UserIdentifierBinding
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import dev3.blockchainapiservice.features.api.access.model.result.UserIdentifier
import dev3.blockchainapiservice.features.api.access.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.features.payout.model.params.FetchMerkleTreeParams
import dev3.blockchainapiservice.features.payout.model.params.GetPayoutsForInvestorParams
import dev3.blockchainapiservice.features.payout.model.response.InvestorPayoutResponse
import dev3.blockchainapiservice.features.payout.model.response.InvestorPayoutsResponse
import dev3.blockchainapiservice.features.payout.repository.MerkleTreeRepository
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class ClaimablePayoutsController(
    private val blockchainService: BlockchainService,
    private val merkleTreeRepository: MerkleTreeRepository
) {

    @Suppress("LongParameterList")
    @GetMapping("/v1/claimable-payouts")
    fun getPayoutsForInvestor(
        @RequestParam(required = true) chainId: Long,
        @ValidEthAddress @RequestParam(required = true) payoutManager: String,
        @UserIdentifierBinding userIdentifier: UserIdentifier
    ): ResponseEntity<InvestorPayoutsResponse> {
        val chainIdValue = ChainId(chainId)
        val payouts = (userIdentifier as? UserWalletAddressIdentifier)?.walletAddress?.let {
            blockchainService.getPayoutsForInvestor(
                chainSpec = ChainSpec(chainIdValue, null),
                GetPayoutsForInvestorParams(
                    payoutManager = ContractAddress(payoutManager),
                    investor = it
                )
            )
        }.orEmpty()

        val merkleTreeParams = payouts.mapTo(HashSet()) {
            FetchMerkleTreeParams(it.payout.assetSnapshotMerkleRoot, chainIdValue, it.payout.asset)
        }
        val merkleTrees = merkleTreeParams.mapNotNull { merkleTreeRepository.fetchTree(it)?.tree }
            .associateBy { it.root.hash }

        val investorPayouts = payouts.mapNotNull { payoutData ->
            val tree = merkleTrees[payoutData.payout.assetSnapshotMerkleRoot]
            val accountBalance = tree?.leafNodesByAddress?.get(payoutData.investor)?.value?.data
            val path = accountBalance?.let { tree.pathTo(it) }

            if (path != null) { // return only claimable (and already claimed) payouts for this investor
                val totalRewardAmount = payoutData.payout.totalRewardAmount.rawValue
                val balance = accountBalance.balance.rawValue
                val totalAssetAmount = payoutData.payout.totalAssetAmount.rawValue
                val totalAmountClaimable = (totalRewardAmount * balance) / totalAssetAmount
                val amountClaimable = totalAmountClaimable - payoutData.amountClaimed.rawValue

                val payout = payoutData.payout.toPayoutResponse()

                InvestorPayoutResponse(
                    payout = payout,
                    investor = payoutData.investor.rawValue,
                    amountClaimed = payoutData.amountClaimed.rawValue,

                    amountClaimable = amountClaimable,
                    balance = accountBalance.balance.rawValue,
                    path = path
                )
            } else null
        }

        return ResponseEntity.ok(InvestorPayoutsResponse(investorPayouts))
    }
}
