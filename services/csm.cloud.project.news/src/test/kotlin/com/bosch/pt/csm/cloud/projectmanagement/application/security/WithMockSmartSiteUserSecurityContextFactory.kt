/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import java.util.UUID.randomUUID
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory
import org.springframework.test.util.ReflectionTestUtils

class WithMockSmartSiteUserSecurityContextFactory :
    WithSecurityContextFactory<WithMockSmartSiteUser> {

  override fun createSecurityContext(withUser: WithMockSmartSiteUser): SecurityContext {
    val principalIdentifier =
        if (withUser.identifier.isBlank()) randomUUID() else withUser.identifier.toUUID()

    val userId = withUser.userId.ifBlank { withUser.value }

    val principal = User(userId, principalIdentifier, withUser.admin, withUser.locked)
    ReflectionTestUtils.setField(principal, "id", withUser.id)

    val authentication: Authentication =
        UsernamePasswordAuthenticationToken(principal, principal.password, principal.authorities)

    return SecurityContextHolder.createEmptyContext().apply { this.authentication = authentication }
  }
}
