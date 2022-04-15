package com.ampnet.blockchainapiservice.model

data class SendScreenConfig(
    val title: String?,
    val message: String?,
    val logo: String?
) {
    companion object {
        val EMPTY = SendScreenConfig(null, null, null)
    }

    fun orEmpty(): SendScreenConfig? =
        if (this == EMPTY) null else this
}
