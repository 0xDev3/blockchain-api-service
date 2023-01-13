package dev3.blockchainapiservice.features.payout.model.request

import dev3.blockchainapiservice.config.validation.MaxStringSize
import dev3.blockchainapiservice.config.validation.ValidEthAddress
import dev3.blockchainapiservice.config.validation.ValidUint256
import java.math.BigInteger
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class CreateAssetSnapshotRequest(
    @field:NotNull
    @field:MaxStringSize
    val name: String,
    @field:NotNull
    @field:ValidEthAddress
    val assetAddress: String,
    @field:NotNull
    @field:ValidUint256
    val payoutBlockNumber: BigInteger,
    @field:NotNull
    @field:Valid
    val ignoredHolderAddresses: Set<@NotNull @ValidEthAddress String>
)
