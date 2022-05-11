package com.ampnet.blockchainapiservice.model

data class ScreenConfig(
    val beforeActionMessage: String?,
    val afterActionMessage: String?
) {
    companion object {
        val EMPTY = ScreenConfig(null, null)
    }

    fun orEmpty(): ScreenConfig? =
        if (this == EMPTY) null else this
}
