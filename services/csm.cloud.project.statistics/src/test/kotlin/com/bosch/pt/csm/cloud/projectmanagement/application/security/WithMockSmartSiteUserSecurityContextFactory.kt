/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.User
import java.util.Locale
import java.util.UUID
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
    val username = withUser.username.ifBlank { withUser.value }

    val principalIdentifier =
        if (withUser.identifier.isBlank()) randomUUID()
        else UUID.fromString(withUser.identifier)
    val principal =
        User(username, principalIdentifier, withUser.isAdmin, withUser.isLocked, Locale.UK)

    ReflectionTestUtils.setField(principal, "id", withUser.id)
    val authentication: Authentication =
        UsernamePasswordAuthenticationToken(principal, principal.password, principal.authorities)
    val context = SecurityContextHolder.createEmptyContext()
    context.authentication = authentication
    return context
  }
}
