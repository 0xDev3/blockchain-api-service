package com.ampnet.blockchainapiservice.model

data class ScreenConfig(
    val title: String?,
    val message: String?,
    val logo: String?
) {
    companion object {
        val EMPTY = ScreenConfig(null, null, null)
    }

    fun orEmpty(): ScreenConfig? =
        if (this == EMPTY) null else this
}
