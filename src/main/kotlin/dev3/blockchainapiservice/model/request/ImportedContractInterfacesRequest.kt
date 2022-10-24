package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.config.validation.MaxArgsSize
import com.ampnet.blockchainapiservice.config.validation.MaxStringSize
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
