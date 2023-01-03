package dev3.blockchainapiservice.features.contract.interfaces.model.result

import dev3.blockchainapiservice.model.json.InterfaceManifestJsonWithId
import dev3.blockchainapiservice.util.InterfaceId

data class MatchingContractInterfaces(
    val manifests: List<InterfaceManifestJsonWithId>,
    val bestMatchingInterfaces: List<InterfaceId>
)
