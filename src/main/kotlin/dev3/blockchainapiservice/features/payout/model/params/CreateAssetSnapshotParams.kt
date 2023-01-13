package dev3.blockchainapiservice.features.payout.model.params

import dev3.blockchainapiservice.generated.jooq.id.ProjectId
import dev3.blockchainapiservice.util.BlockNumber
import dev3.blockchainapiservice.util.ChainId
import dev3.blockchainapiservice.util.ContractAddress
import dev3.blockchainapiservice.util.WalletAddress

data class CreateAssetSnapshotParams(
    val name: String,
    val chainId: ChainId,
    val projectId: ProjectId,
    val assetContractAddress: ContractAddress,
    val payoutBlock: BlockNumber,
    val ignoredHolderAddresses: Set<WalletAddress>
)
