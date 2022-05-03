package com.ampnet.blockchainapiservice.util

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.exception.IncompleteRequestException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ResolvableTest : TestBase() {

    @Test
    fun mustReturnProvidedValueWhenNonNull() {
        val resolvable = Resolvable("alt", "message")
        val value = "value"

        verify("provided value is returned") {
            assertThat(resolvable.resolve(value)).withMessage()
                .isEqualTo(value)
        }
    }

    @Test
    fun mustReturnAlternativeValueWhenProvidedValueIsNull() {
        val alt = "alt"
        val resolvable = Resolvable(alt, "message")

        verify("alternative value is returned") {
            assertThat(resolvable.resolve(null)).withMessage()
                .isEqualTo(alt)
        }
    }

    @Test
    fun mustThrowIncompleteRequestExceptionWhenBothValuesAreNull() {
        val resolvable = Resolvable(null, "message")

        verify("IncompleteRequestException is thrown") {
            assertThrows<IncompleteRequestException>(message) {
                resolvable.resolve(null)
            }
        }
    }
}
