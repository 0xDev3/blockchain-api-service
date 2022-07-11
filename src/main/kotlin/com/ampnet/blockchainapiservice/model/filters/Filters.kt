package com.ampnet.blockchainapiservice.model.filters

@JvmInline
value class AndList<T>(val list: List<T>) {
    constructor(vararg values: T) : this(values.toList())
}

@JvmInline
value class OrList<T>(val list: List<T>) {
    constructor(vararg values: T) : this(values.toList())
}
