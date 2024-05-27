/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.test

import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList
import org.springframework.security.core.context.SecurityContextHolder

object AuthorizationTestUtils {

  fun <R> doWithAuthorization(user: User?, isAdmin: Boolean = false, procedure: () -> R): R {
    SecurityContextHolder.clearContext()
    if (user != null) {
      authorizeWithUser(user, isAdmin)
    } else {
      authorizeWithAnonymousUser()
    }
    return procedure.invoke().also { SecurityContextHolder.clearContext() }
  }

  fun authorizeWithUser(user: User) {
    authorizeWithUser(user, false)
  }

  private fun authorizeWithUser(user: User, isAdmin: Boolean) {
    if (isAdmin) {
      SecurityContextHolder.getContext().authentication =
          UsernamePasswordAuthenticationToken(
              user, "n/a", createAuthorityList("ROLE_USER", "ROLE_ADMIN"))
    } else {
      SecurityContextHolder.getContext().authentication =
          UsernamePasswordAuthenticationToken(user, "n/a", createAuthorityList("ROLE_USER"))
    }
  }

  private fun authorizeWithAnonymousUser() {
    SecurityContextHolder.getContext().authentication =
        AnonymousAuthenticationToken("n/a", "n/a", createAuthorityList("USER"))
  }
}
