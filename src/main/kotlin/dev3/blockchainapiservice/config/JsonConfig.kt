package dev3.blockchainapiservice.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.addDeserializer
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import dev3.blockchainapiservice.features.contract.abi.model.Tuple
import dev3.blockchainapiservice.features.contract.readcall.model.params.OutputParameter
import dev3.blockchainapiservice.features.functions.encoding.model.FunctionArgument
import dev3.blockchainapiservice.features.payout.util.MerkleHash
import dev3.blockchainapiservice.features.payout.util.MerkleTree
import dev3.blockchainapiservice.features.payout.util.json.MerkleHashJsonSerializer
import dev3.blockchainapiservice.features.payout.util.json.MerkleTreeJsonSerializer
import dev3.blockchainapiservice.generated.jooq.id.GeneratedIdsJacksonModule
import dev3.blockchainapiservice.util.json.FunctionArgumentJsonDeserializer
import dev3.blockchainapiservice.util.json.OutputParameterJsonDeserializer
import dev3.blockchainapiservice.util.json.TupleSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.math.BigDecimal
import java.math.BigInteger

@Configuration
class JsonConfig {

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        val mapper = ObjectMapper()

        mapper.propertyNamingStrategy = PropertyNamingStrategies.SnakeCaseStrategy()
        mapper.registerModule(JavaTimeModule())
        mapper.registerModule(GeneratedIdsJacksonModule)
        mapper.registerModule(ParameterNamesModule())
        mapper.registerModule(
            SimpleModule().apply {
                addDeserializer(FunctionArgument::class, FunctionArgumentJsonDeserializer())
                addDeserializer(OutputParameter::class, OutputParameterJsonDeserializer())
                addSerializer(MerkleTree::class.java, MerkleTreeJsonSerializer())
                addSerializer(MerkleHash::class.java, MerkleHashJsonSerializer())
            }
        )
        mapper.registerModule(
            SimpleModule().apply {
                addSerializer(BigDecimal::class.java, ToStringSerializer())
                addSerializer(BigInteger::class.java, ToStringSerializer())
                addSerializer(Tuple::class.java, TupleSerializer())
            }
        )
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        return mapper.registerModule(KotlinModule.Builder().build())
    }
}
