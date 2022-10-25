package dev3.blockchainapiservice.security

import dev3.blockchainapiservice.testcontainers.HardhatTestContainer
import org.springframework.security.test.context.support.WithSecurityContext

@Retention(value = AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@WithSecurityContext(factory = WithMockUserSecurityFactory::class)
annotation class WithMockUser(
    val address: String = HardhatTestContainer.ACCOUNT_ADDRESS_1
)
