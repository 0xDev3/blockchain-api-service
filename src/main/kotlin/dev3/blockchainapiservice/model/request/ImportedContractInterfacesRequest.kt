package dev3.blockchainapiservice.model.request

import dev3.blockchainapiservice.config.validation.MaxArgsSize
import dev3.blockchainapiservice.config.validation.MaxStringSize
import org.springframework.validation.annotation.Validated
import javax.validation.Valid
import javax.validation.constraints.NotNull

@Validated
data class ImportedContractInterfacesRequest(
    @field:Valid
    @field:NotNull
    @field:MaxArgsSize
    val interfaces: List<@MaxStringSize String>
)
