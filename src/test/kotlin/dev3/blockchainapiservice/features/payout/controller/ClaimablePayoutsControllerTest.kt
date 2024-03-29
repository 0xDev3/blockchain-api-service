package dev3.blockchainapiservice.features.payout.controller

import dev3.blockchainapiservice.JsonSchemaDocumentation
import dev3.blockchainapiservice.TestBase
import dev3.blockchainapiservice.blockchain.BlockchainService
import dev3.blockchainapiservice.blockchain.properties.ChainSpec
import dev3.blockchainapiservice.features.api.access.model.result.UserWalletAddressIdentifier
import dev3.blockchainapiservice.features.payout.model.params.FetchMerkleTreeParams
import dev3.blockchainapiservice.features.payout.model.params.GetPayoutsForInvestorParams
import dev3.blockchainapiservice.features.payout.model.response.InvestorPayoutResponse
import dev3.blockchainapiservice.features.payout.model.response.InvestorPayoutsResponse
import dev3.blockchainapiservice.features.payout.model.result.MerkleTreeWithId
import dev3.blockchainapiservice.features.payout.model.result.Payout
import dev3.blockchainapiservice.features.payout.model.result.PayoutForInvestor
import dev3.blockchainapiservice.features.payout.repository.MerkleTreeRepository
import dev3.blockchainapiservice.features.payout.util.HashFunction
import dev3.blockchainapiservice.features.payout.util.IpfsHash
import dev3.blockchainapiservice.features.payout.util.MerkleHash
import dev3.blockchainapiservice.features.payout.util.MerkleTree
import dev3.blockchainapiservice.features.payout.util.PayoutAccountBalance
import dev3.blockchainapiservice.generated.jooq.id.MerkleTreeRootId
import dev3.blockchainapiservice.generated.jooq.id.UserId
import dev3.blockchainapiservice.util.Balance
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.WalletAddress
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID

class ClaimablePayoutsControllerTest : TestBase() {

    @Test
    fun mustCorrectlyFetchPayoutsForInvestor() {
        val chainSpec = ChainSpec(ChainId(123L), null)
        val params = GetPayoutsForInvestorParams(
            payoutManager = ContractAddress("d"),
            investor = WalletAddress("1")
        )
        val accountBalances = listOf(
            PayoutAccountBalance(params.investor, Balance(BigInteger("100"))),
            PayoutAccountBalance(params.investor, Balance(BigInteger("200"))),
            PayoutAccountBalance(WalletAddress("2"), Balance(BigInteger("300")))
        )
        val trees = listOf(
            MerkleTree(listOf(accountBalances[0]), HashFunction.KECCAK_256),
            MerkleTree(listOf(accountBalances[1]), HashFunction.KECCAK_256),
            MerkleTree(listOf(accountBalances[2]), HashFunction.KECCAK_256)
        )
        val payouts = listOf(
            createPayout(0, trees[0].root.hash, asset = BigInteger("1000"), reward = BigInteger("1000")),
            createPayout(1, trees[1].root.hash, asset = BigInteger("2000"), reward = BigInteger("4000")),
            createPayout(2, trees[2].root.hash, asset = BigInteger("3000"), reward = BigInteger("9000"))
        )
        val payoutsForInvestor = listOf(
            PayoutForInvestor(
                payout = payouts[0],
                investor = params.investor,
                amountClaimed = Balance(BigInteger.ZERO) // not claimed
            ),
            PayoutForInvestor(
                payout = payouts[1],
                investor = params.investor,
                amountClaimed = Balance(BigInteger("400")) // fully claimed, 10% of 4000 (200 / 2000 * 4000)
            ),
            PayoutForInvestor(
                payout = payouts[2],
                investor = params.investor,
                amountClaimed = Balance(BigInteger.ZERO) // not claimable at all for this investor
            )
        )
        val blockchainService = mock<BlockchainService>()

        suppose("some payouts are returned for investor") {
            call(blockchainService.getPayoutsForInvestor(chainSpec, params))
                .willReturn(payoutsForInvestor)
        }

        val merkleTreeRepository = mock<MerkleTreeRepository>()

        suppose("some Merkle trees will be returned") {
            call(
                merkleTreeRepository.fetchTree(
                    FetchMerkleTreeParams(trees[0].root.hash, chainSpec.chainId, payouts[0].asset)
                )
            ).willReturn(MerkleTreeWithId(MerkleTreeRootId(UUID.randomUUID()), trees[0]))

            call(
                merkleTreeRepository.fetchTree(
                    FetchMerkleTreeParams(trees[1].root.hash, chainSpec.chainId, payouts[1].asset)
                )
            ).willReturn(MerkleTreeWithId(MerkleTreeRootId(UUID.randomUUID()), trees[1]))

            call(
                merkleTreeRepository.fetchTree(
                    FetchMerkleTreeParams(trees[2].root.hash, chainSpec.chainId, payouts[2].asset)
                )
            ).willReturn(MerkleTreeWithId(MerkleTreeRootId(UUID.randomUUID()), trees[2]))
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = params.investor,
            stripeClientId = null
        )

        val controller = ClaimablePayoutsController(blockchainService, merkleTreeRepository)

        verify("correct investor payout states are returned") {
            val response = controller.getPayoutsForInvestor(
                chainId = chainSpec.chainId.value,
                payoutManager = params.payoutManager.rawValue,
                userIdentifier = userIdentifier
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        InvestorPayoutsResponse(
                            listOf(
                                InvestorPayoutResponse(
                                    payout = payouts[0].toPayoutResponse(),
                                    investor = params.investor.rawValue,
                                    amountClaimed = BigInteger.ZERO,
                                    amountClaimable = BigInteger("100"),
                                    balance = accountBalances[0].balance.rawValue,
                                    path = trees[0].pathTo(accountBalances[0])!!
                                ),
                                InvestorPayoutResponse(
                                    payout = payouts[1].toPayoutResponse(),
                                    investor = params.investor.rawValue,
                                    amountClaimed = BigInteger("400"),
                                    amountClaimable = BigInteger.ZERO,
                                    balance = accountBalances[1].balance.rawValue,
                                    path = trees[1].pathTo(accountBalances[1])!!
                                ) // payouts[2] is not claimable by this investor
                            )
                        )
                    )
                )
        }
    }

    private fun createPayout(id: Long, rootHash: MerkleHash, asset: BigInteger, reward: BigInteger): Payout =
        Payout(
            payoutId = BigInteger.valueOf(id),
            payoutOwner = WalletAddress("aaa$id"),
            payoutInfo = "payout-info-$id",
            isCanceled = false,
            asset = ContractAddress("bbb$id"),
            totalAssetAmount = Balance(asset),
            ignoredHolderAddresses = emptySet(),
            assetSnapshotMerkleRoot = rootHash,
            assetSnapshotMerkleDepth = BigInteger.valueOf(id),
            assetSnapshotBlockNumber = BlockNumber(BigInteger.valueOf(id * 100)),
            assetSnapshotMerkleIpfsHash = IpfsHash("ipfs-hash-$id"),
            rewardAsset = ContractAddress("ccc$id"),
            totalRewardAmount = Balance(reward),
            remainingRewardAmount = Balance(reward)
        )
}
