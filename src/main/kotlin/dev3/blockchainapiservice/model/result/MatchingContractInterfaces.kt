package dev3.blockchainapiservice.model.result

import dev3.blockchainapiservice.model.json.InterfaceManifestJsonWithId
import dev3.blockchainapiservice.util.InterfaceId

data class MatchingContractInterfaces(
    val manifests: List<InterfaceManifestJsonWithId>,
    val bestMatchingInterfaces: List<InterfaceId>
)
