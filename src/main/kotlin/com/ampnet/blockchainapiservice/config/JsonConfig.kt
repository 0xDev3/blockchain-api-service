package com.ampnet.blockchainapiservice.config

import com.ampnet.blockchainapiservice.model.params.OutputParameter
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.Tuple
import com.ampnet.blockchainapiservice.util.json.FunctionArgumentJsonDeserializer
import com.ampnet.blockchainapiservice.util.json.OutputParameterJsonDeserializer
import com.ampnet.blockchainapiservice.util.json.TupleSerializer
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.addDeserializer
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
        mapper.registerModule(
            SimpleModule().apply {
                addDeserializer(FunctionArgument::class, FunctionArgumentJsonDeserializer())
                addDeserializer(OutputParameter::class, OutputParameterJsonDeserializer())
            }
        )
        mapper.registerModule(
            SimpleModule().apply {
                addSerializer(BigInteger::class.java, ToStringSerializer())
                addSerializer(BigDecimal::class.java, ToStringSerializer())
                addSerializer(Tuple::class.java, TupleSerializer())
            }
        )
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return mapper.registerModule(KotlinModule.Builder().build())
    }
}
