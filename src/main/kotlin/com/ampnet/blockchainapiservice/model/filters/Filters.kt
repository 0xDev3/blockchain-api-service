package com.ampnet.blockchainapiservice.model.filters

@JvmInline
value class AndList<T>(val list: List<T>)

@JvmInline
value class OrList<T>(val list: List<T>)
