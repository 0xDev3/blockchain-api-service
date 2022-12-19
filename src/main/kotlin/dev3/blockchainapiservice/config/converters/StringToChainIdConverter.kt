package dev3.blockchainapiservice.config.converters

import dev3.blockchainapiservice.util.ChainId
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
@ConfigurationPropertiesBinding
class StringToChainIdConverter : Converter<String, ChainId> {
    override fun convert(source: String): ChainId = ChainId(source.toLong())
}
