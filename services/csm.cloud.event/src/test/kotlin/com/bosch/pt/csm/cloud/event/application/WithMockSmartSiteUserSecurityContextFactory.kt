/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2016
 *
 * *************************************************************************
 */
package com.bosch.pt.csm.cloud.event.application

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.event.user.model.User
import java.util.UUID.randomUUID
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory

/** A [WithMockSmartSiteUserSecurityContextFactory] that works with [ ]. */
internal class WithMockSmartSiteUserSecurityContextFactory :
    WithSecurityContextFactory<WithMockSmartSiteUser> {

  override fun createSecurityContext(withUser: WithMockSmartSiteUser): SecurityContext {
    val grantedAuthorities = createAuthorityList("ROLE_USER")
    val identifier =
        if (withUser.identifier.isNotBlank()) {
          withUser.identifier.toUUID()
        } else {
          randomUUID()
        }

    val authentication: Authentication =
        User(identifier, grantedAuthorities).let {
          UsernamePasswordAuthenticationToken(it, it.getPassword(), it.getAuthorities())
        }

    return SecurityContextHolder.createEmptyContext().apply { this.authentication = authentication }
  }
}
