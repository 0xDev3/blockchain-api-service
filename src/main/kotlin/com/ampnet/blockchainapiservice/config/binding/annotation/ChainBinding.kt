package com.ampnet.blockchainapiservice.config.binding.annotation

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ChainBinding(val chainIdPathVariable: String = "chainId")
