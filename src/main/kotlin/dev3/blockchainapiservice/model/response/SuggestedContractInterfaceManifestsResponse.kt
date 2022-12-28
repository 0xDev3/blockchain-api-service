package dev3.blockchainapiservice.model.response

import dev3.blockchainapiservice.model.result.MatchingContractInterfaces

data class SuggestedContractInterfaceManifestsResponse(
    val manifests: List<ContractInterfaceManifestResponse>,
    val bestMatchingInterfaces: List<String>
) {
    constructor(matchingInterfaces: MatchingContractInterfaces) : this(
        manifests = matchingInterfaces.manifests.map(::ContractInterfaceManifestResponse),
        bestMatchingInterfaces = matchingInterfaces.bestMatchingInterfaces.map { it.value }
    )
}
