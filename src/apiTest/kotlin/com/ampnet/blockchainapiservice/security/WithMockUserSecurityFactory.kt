package com.ampnet.blockchainapiservice.security

import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory

class WithMockUserSecurityFactory : WithSecurityContextFactory<WithMockUser> {

    override fun createSecurityContext(annotation: WithMockUser): SecurityContext {
        val token = TestingAuthenticationToken(annotation.address, "password")
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = token
        return context
    }
}
